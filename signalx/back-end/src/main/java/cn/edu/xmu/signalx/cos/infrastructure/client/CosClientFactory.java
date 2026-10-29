package cn.edu.xmu.signalx.cos.infrastructure.client;

import cn.edu.xmu.signalx.cos.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * COS客户端工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CosClientFactory {

    private final CosConfig cosConfig;
    private COSClient cosClient;

    /**
     * 初始化COS客户端
     */
    @PostConstruct
    public void init() {
        if (!cosConfig.isEnabled()) {
            log.warn("COS存储未启用");
            return;
        }

        try {
            log.info("初始化COS客户端...");

            // 1. 初始化用户身份信息
            COSCredentials cred = new BasicCOSCredentials(
                    cosConfig.getSecretId(),
                    cosConfig.getSecretKey()
            );

            // 2. 设置bucket的区域
            ClientConfig clientConfig = new ClientConfig(new Region(cosConfig.getRegion()));

            // 3. 配置超时时间
            clientConfig.setSocketTimeout(30 * 1000);
            clientConfig.setConnectionTimeout(30 * 1000);

            // 4. 生成cos客户端
            this.cosClient = new COSClient(cred, clientConfig);

            log.info("COS客户端初始化成功");

        } catch (Exception e) {
            log.error("COS客户端初始化失败，COS功能将不可用", e);
            // 不抛出异常，保持cosClient为null，这样isAvailable()方法会返回false
            // 这样即使COS初始化失败，应用也能正常启动
        }
    }

    /**
     * 获取COS客户端
     */
    public COSClient getCosClient() {
        if (!cosConfig.isEnabled()) {
            throw new IllegalStateException("COS存储未启用");
        }
        if (cosClient == null) {
            throw new IllegalStateException("COS客户端未初始化");
        }
        return cosClient;
    }

    /**
     * 检查服务是否可用
     */
    public boolean isAvailable() {
        return cosConfig.isEnabled() && cosClient != null;
    }

    /**
     * 关闭客户端
     */
    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
            log.info("COS客户端已关闭");
        }
    }
}
