package cn.edu.xmu.signalx.record.infrastructure.crawler;

import cn.edu.xmu.signalx.core.config.CrawlerTriggerProperties;
import cn.edu.xmu.signalx.record.port.CrawlerRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class CrawlerRunnerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "crawler.trigger", name = "url")
    public CrawlerRunner httpCrawlerRunner(RestTemplateBuilder builder, CrawlerTriggerProperties properties) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
        return new HttpCrawlerRunner(restTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean(CrawlerRunner.class)
    public CrawlerRunner noopCrawlerRunner() {
        return new NoopCrawlerRunner();
    }
}
