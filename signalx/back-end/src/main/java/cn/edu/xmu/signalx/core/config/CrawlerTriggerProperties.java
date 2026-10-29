package cn.edu.xmu.signalx.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "crawler.trigger")
public class CrawlerTriggerProperties {
    /**
     * External crawler trigger URL.
     * Example: http://localhost:9000/api/crawler/trigger
     */
    private String url;

    /**
     * Optional callback URL for the crawler to POST crawled news back to this backend.
     * Example: http://backend-host:8080/api/v1/internal/crawler/news/batchUpsert
     */
    private String callbackUrl;

    /** Shared token used by crawler when calling callbackUrl (sent in header X-Crawler-Token). */
    private String callbackToken;

    /**
     * HTTP method used to trigger crawling: POST (default) or GET.
     */
    private String method = "POST";

    /**
     * Openclaw trigger URL.
     * Example: http://localhost:8000/api/v1/openclaw/crawl
     */
    private String openclawUrl;

    /**
     * Openclaw report URL.
     * Example: http://localhost:8000/api/v1/openclaw/report
     */
    private String openclawReportUrl;

    /** Connect timeout in milliseconds. */
    private long connectTimeoutMs = 2000;

    /** Read timeout in milliseconds. */
    private long readTimeoutMs = 300000;
}
