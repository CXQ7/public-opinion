package cn.edu.xmu.signalx.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class AIProperties {
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private Double temperature = 0.7;
}
