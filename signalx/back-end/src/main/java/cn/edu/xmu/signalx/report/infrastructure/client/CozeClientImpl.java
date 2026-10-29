package cn.edu.xmu.signalx.report.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CozeClientImpl implements CozeClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${coze.api.base-url}")
    private String baseUrl;

    @Value("${coze.api.token}")
    private String token;

    @Override
    public String submitTask(List<String> keywords, List<Map<String, Object>> newsList) {
        String url = baseUrl + "/api/execute";

        Map<String, Object> body = new HashMap<>();
        body.put("keywords", keywords);
        body.put("news_list", newsList);
        log.info("CozeClient提交的关键词参数 | keywords={}", keywords);
        log.info("CozeClient请求体完整内容 | body={}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isEmpty() && !"your_token_here".equals(token)) {
            headers.set("Authorization", "Bearer " + token);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            Map result = restTemplate.postForObject(url, entity, Map.class);
            if (result != null) {
                Object runId = result.get("run_id");
                if (runId == null) runId = result.get("id");
                if (runId != null) {
                    log.info("Coze任务提交成功 | runId={}", runId);
                    return runId.toString();
                }
                log.warn("Coze返回结果无run_id/id字段 | 响应={}", result);
            } else {
                log.warn("Coze返回结果为空");
            }
        } catch (Exception e) {
            log.error("Coze任务提交失败 | url={}", url, e);
        }
        return null;
    }

    @Override
    public CozeResult query(String runId) {
        String url = baseUrl + "/api/query/" + runId;

        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isEmpty() && !"your_token_here".equals(token)) {
            headers.set("Authorization", "Bearer " + token);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map result = response.getBody();
            if (result == null) {
                log.warn("Coze查询结果为空 | runId={}", runId);
                return CozeResult.failed();
            }

            String status = (String) result.get("status");
            if (status == null) {
                log.warn("Coze查询无status字段 | runId={}", runId);
                return CozeResult.running();
            }

            if ("completed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                String reportContent = "";
                Object resultObj = result.get("result");
                if (resultObj instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    Object report = resultMap.get("report");
                    if (report != null) {
                        reportContent = report.toString();
                    }
                }
                log.info("Coze任务完成 | runId={} | 报表内容长度={}", runId, reportContent.length());
                return CozeResult.completed(reportContent);
            } else if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                log.error("Coze任务失败 | runId={} | 状态={}", runId, status);
                return CozeResult.failed();
            } else if ("running".equalsIgnoreCase(status) || "processing".equalsIgnoreCase(status)) {
                log.debug("Coze任务运行中 | runId={} | 状态={}", runId, status);
                return CozeResult.running();
            }

            log.warn("Coze未知状态 | runId={} | 状态={}", runId, status);
            return CozeResult.running();
        } catch (Exception e) {
            log.error("Coze查询失败 | url={}", url, e);
            return CozeResult.failed();
        }
    }
}
