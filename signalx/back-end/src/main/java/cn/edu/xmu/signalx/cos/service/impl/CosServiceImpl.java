package cn.edu.xmu.signalx.cos.service.impl;

import cn.edu.xmu.signalx.cos.config.CosConfig;
import cn.edu.xmu.signalx.cos.service.CosService;
import cn.edu.xmu.signalx.cos.infrastructure.client.CosClientFactory;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * COS存储服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosServiceImpl implements CosService {

    private final CosClientFactory cosClientFactory;
    private final CosConfig cosConfig;

    // 线程安全的日期时间格式化器
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");

    @Override
    public String storeReport(String reportTitle, String reportContent, String userId) {
        // 参数验证
        if (reportTitle == null || reportTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("报告标题不能为空");
        }
        if (reportContent == null) {
            throw new IllegalArgumentException("报告内容不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            // 生成标准的报告文件名
            String fileName = generateReportFileName(reportTitle);

            // 构建完整的存储路径（带用户/日期目录结构）
            String objectKey = buildReportObjectKey(fileName,userId);

            COSClient cosClient = cosClientFactory.getCosClient();

            // 转换为字节流
            byte[] bytes = reportContent.getBytes(StandardCharsets.UTF_8);

            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType("text/markdown; charset=utf-8");

            // 大文件处理：如果文件大小超过5MB，使用分块上传
            final long CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
            if (bytes.length > CHUNK_SIZE) {
                log.info("使用分块上传: 文件大小={} 字节", bytes.length);
                try {
                    // 1. 初始化分块上传
                    InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
                            cosConfig.getBucketName(),
                            objectKey,
                            metadata
                    );
                    // 设置公共读权限
                    initRequest.withCannedACL(CannedAccessControlList.PublicRead);
                    InitiateMultipartUploadResult initResult = cosClient.initiateMultipartUpload(initRequest);
                    String uploadId = initResult.getUploadId();

                    // 2. 分块上传
                    List<PartETag> partETags = new ArrayList<>();
                    long filePosition = 0;
                    int partNumber = 1;

                    while (filePosition < bytes.length) {
                        // 计算当前块大小
                        long currentChunkSize = Math.min(CHUNK_SIZE, bytes.length - filePosition);
                        byte[] chunkBytes = new byte[(int) currentChunkSize];
                        System.arraycopy(bytes, (int) filePosition, chunkBytes, 0, (int) currentChunkSize);

                        try (ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunkBytes)) {
                            // 上传当前块
                            UploadPartRequest uploadRequest = new UploadPartRequest()
                                    .withBucketName(cosConfig.getBucketName())
                                    .withKey(objectKey)
                                    .withUploadId(uploadId)
                                    .withPartNumber(partNumber)
                                    .withInputStream(chunkStream)
                                    .withPartSize(currentChunkSize);

                            UploadPartResult uploadResult = cosClient.uploadPart(uploadRequest);
                            partETags.add(uploadResult.getPartETag());
                        }

                        filePosition += currentChunkSize;
                        partNumber++;
                    }

                    // 3. 完成分块上传
                    CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                            cosConfig.getBucketName(),
                            objectKey,
                            uploadId,
                            partETags
                    );
                    cosClient.completeMultipartUpload(completeRequest);
                    log.info("分块上传成功: {}，共{}块", objectKey, partETags.size());

                } catch (Exception e) {
                    log.error("分块上传失败: {}", e.getMessage(), e);
                    throw new RuntimeException("分块上传失败: " + e.getMessage(), e);
                }
            } else {
                // 小文件使用普通上传
                try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
                    // 创建上传请求
                    PutObjectRequest request = new PutObjectRequest(
                            cosConfig.getBucketName(),
                            objectKey,
                            stream,
                            metadata
                    );
                    // 设置公共读权限
                    request.setCannedAcl(CannedAccessControlList.PublicRead);

                    // 执行上传
                    cosClient.putObject(request);
                }
            }

            // 生成访问URL
            String fileUrl = buildFileUrl(objectKey);

            log.info("报告存储成功: {}, 大小: {} 字节", objectKey, bytes.length);
            return fileUrl;

        } catch (Exception e) {
            throw new RuntimeException("存储报告失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        // 参数验证
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("文件路径不能为空");
            return false;
        }

        try {
            if (!isAvailable()) {
                log.warn("COS服务不可用，无法删除文件: {}", filePath);
                return false;
            }

            COSClient cosClient = cosClientFactory.getCosClient();

            // 处理文件路径
            String objectKey;
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                // 如果是完整URL，提取objectKey部分
                // URL格式: https://bucket-name.cos.region.myqcloud.com/object-key
                String urlPrefix = String.format("https://%s.cos.%s.myqcloud.com/",
                        cosConfig.getBucketName(), cosConfig.getRegion());
                if (filePath.startsWith(urlPrefix)) {
                    objectKey = filePath.substring(urlPrefix.length());
                } else {
                    // 如果URL格式不匹配，尝试从最后一个斜杠后提取
                    int lastSlashIndex = filePath.lastIndexOf('/');
                    if (lastSlashIndex > 0) {
                        objectKey = filePath.substring(lastSlashIndex + 1);
                    } else {
                        objectKey = filePath;
                    }
                }
            } else if (filePath.startsWith("reports/")) {
                // 如果已经是完整路径，直接使用
                objectKey = filePath;
            } else {
                // 否则添加basePath前缀
                objectKey = cosConfig.getBasePath() + filePath;
            }

            // 删除文件
            cosClient.deleteObject(cosConfig.getBucketName(), objectKey);
            log.info("文件删除成功: {}", objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件删除失败: {}, 错误: {}", filePath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return cosClientFactory.isAvailable();
    }

    // ================ 私有工具方法 ================

    /**
     * 生成报告文件名
     * 格式：配置名【keywordA,keywordB,...】报告_随机数.md
     */
    private String generateReportFileName(String reportTitle) {
        LocalDateTime now = LocalDateTime.now();
        String timeStr = TIME_FORMATTER.format(now);
        String random = UUID.randomUUID().toString().substring(0, 6);

        // 清理标题中的特殊字符
        String safeTitle = reportTitle.replaceAll("[\\\\/:*?\"<>|,\\[\\]]", "_");

        // 移除可能的.md扩展名
        if (safeTitle.endsWith(".md")) {
            safeTitle = safeTitle.substring(0, safeTitle.length() - 3);
        }

        // 默认文件名格式
        String fileName = safeTitle + "_" + timeStr + "_" + random + ".md";

        // 如果标题包含关键词信息，尝试解析
        if (reportTitle.contains("【") && reportTitle.contains("】")) {
            try {
                int start = reportTitle.indexOf("【");
                int end = reportTitle.indexOf("】");
                if (start < end) {
                    String configPart = reportTitle.substring(0, start).trim();
                    String keywordsPart = reportTitle.substring(start + 1, end).trim();

                    // 清理配置名和关键词
                    String safeConfig = configPart.replaceAll("[\\\\/:*?\"<>|,]", "_");
                    String safeKeywords = keywordsPart.replaceAll("[\\\\/:*?\"<>|,\\[\\]]", "_");

                    // 将逗号替换为下划线
                    safeKeywords = safeKeywords.replace(",", "_");

                    // 构建格式化的文件名
                    fileName = safeConfig + "【" + safeKeywords + "】报告_" + timeStr + "_" + random + ".md";
                }
            } catch (Exception e) {
                log.warn("解析报告标题失败，使用默认格式: {}", e.getMessage());
            }
        }

        return fileName;
    }

    /**
     * 构建报告对象键（完整的存储路径）
     * 格式：basePath/用户/年份/月份/日期/文件名
     */
    private String buildReportObjectKey(String fileName, String userId) {
        // 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        String year = YEAR_FORMATTER.format(now);
        String month = MONTH_FORMATTER.format(now);
        String day = DAY_FORMATTER.format(now);

        // 使用传入的userId参数
        String userPath = (userId != null && !userId.trim().isEmpty()) ? userId : "default";

        // 构建路径，使用配置的基础路径
        String basePath = cosConfig.getBasePath();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        String path = String.format("%s%s/%s/%s/%s/%s",
                basePath, userPath, year, month, day, fileName);

        return path;
    }

    /**
     * 构建文件访问URL
     */
    private String buildFileUrl(String objectKey) {
        return String.format("https://%s.cos.%s.myqcloud.com/%s",
                cosConfig.getBucketName(),
                cosConfig.getRegion(),
                objectKey);
    }
}
