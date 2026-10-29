package cn.edu.xmu.signalx.record.infrastructure.crawler;

import cn.edu.xmu.signalx.core.config.CrawlerTriggerProperties;
import cn.edu.xmu.signalx.record.port.CrawlerRunner;
import cn.edu.xmu.signalx.task.domain.model.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class HttpCrawlerRunner implements CrawlerRunner {

    private final RestTemplate restTemplate;
    private final CrawlerTriggerProperties properties;

    @Override
    public void run(Task task) {
        String url = properties.getUrl();
        if (url == null || url.isBlank()) {
            log.info("Crawler trigger url is blank; skip crawling for task [{}]", task.getId());
            return;
        }

        String method = properties.getMethod() == null ? "POST" : properties.getMethod().trim().toUpperCase();

        try {
            if ("GET".equals(method)) {
                String requestUrl = UriComponentsBuilder.fromHttpUrl(url)
                        .queryParam("taskId", task.getId())
                        .queryParam("keywords", String.join(",", task.getKeywords()))
                        .build(true)
                        .toUriString();

                ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new RestClientException("Non-2xx response: " + response.getStatusCode());
                }

                log.info("Triggered crawler via GET for task [{}], status={}", task.getId(), response.getStatusCode());
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("taskId", task.getId());
            body.put("userId", task.getUserId());
            body.put("keywords", task.getKeywords());
            // optional hints for crawler side
            if (task.getConfig() != null) {
                body.put("startTime", task.getConfig().getStartTime());
                body.put("endTime", task.getConfig().getEndTime());
                body.put("intervalMinutes", task.getConfig().getInterval());
            }
            body.put("triggerTime", LocalDateTime.now().toString());

            if (properties.getCallbackUrl() != null && !properties.getCallbackUrl().isBlank()) {
                body.put("callbackUrl", properties.getCallbackUrl());
            }
            if (properties.getCallbackToken() != null && !properties.getCallbackToken().isBlank()) {
                body.put("callbackToken", properties.getCallbackToken());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("Non-2xx response: " + response.getStatusCode());
            }

            log.info("Triggered crawler via POST for task [{}], status={}", task.getId(), response.getStatusCode());
        } catch (RestClientException e) {
            // Re-throw to allow nack & retry from the Queue logic!
            log.warn("Failed to trigger crawler via HTTP for task [{}], url={}, message={}",
                    task.getId(), url, e.getMessage());
            throw new RuntimeException("Crawler HTTP trigger failed", e);
        }
    }
}
