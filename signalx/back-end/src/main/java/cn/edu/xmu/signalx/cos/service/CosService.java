package cn.edu.xmu.signalx.cos.service;

/**
 * COS存储服务接口
 */
public interface CosService {

    /**
     * 存储舆情报告
     * @param reportTitle 报告标题
     * @param reportContent 报告内容（Markdown格式）
     * @param userId 用户ID
     * @return 文件访问URL
     */
    String storeReport(String reportTitle, String reportContent, String userId);

    /**
     * 删除文件
     * @param filePath 文件路径（相对于basePath）
     * @return 是否删除成功
     */
    boolean deleteFile(String filePath);

    /**
     * 检查服务状态
     * @return 是否可用
     */
    boolean isAvailable();
}
