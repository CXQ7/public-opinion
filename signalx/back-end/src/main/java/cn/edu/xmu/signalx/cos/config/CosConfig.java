package cn.edu.xmu.signalx.cos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * COS配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "rhythm.cos")
public class CosConfig {

    private String secretId;
    private String secretKey;
    private String region = "ap-guangzhou";
    private String bucketName;
    private String basePath = "reports/";
    private boolean enabled = true;

}
