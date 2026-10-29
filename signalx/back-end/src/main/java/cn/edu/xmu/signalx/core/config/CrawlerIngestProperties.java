package cn.edu.xmu.signalx.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "crawler.ingest")
public class CrawlerIngestProperties {
    /** Enable/disable crawler ingest endpoint. */
    private boolean enabled = true;

    /** Shared token required in header X-Crawler-Token. */
    private String token;
}
