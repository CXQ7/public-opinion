package cn.edu.xmu.signalx.task.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.common.TaskType;
import cn.edu.xmu.signalx.core.config.CrawlerTriggerProperties;
import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.record.service.NewsService;
import cn.edu.xmu.signalx.report.service.ReportService;
import cn.edu.xmu.signalx.task.domain.dto.OpenclawTaskDTO;
import cn.edu.xmu.signalx.task.domain.model.OpenclawTask;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import cn.edu.xmu.signalx.task.domain.repository.OpenclawTaskRepository;
import cn.edu.xmu.signalx.task.domain.vo.OpenclawTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenclawTaskService {

    private final OpenclawTaskRepository openclawTaskRepository;
    private final NewsService newsService;
    private final ReportService reportService;
    private final RestTemplate restTemplate;
    private final CrawlerTriggerProperties crawlerTriggerProperties;

    // 将 @Transactional 移到内部方法或确保不会导致异步调用失效
    @Async
    public void executeAndReportAsync(OpenclawTask task) {
        log.info("Starting Openclaw task flow ASYNC | taskId: {}, name: {}", task.getId(), task.getName());
        executeAndReport(task);
    }

    @Transactional
    public void executeAndReport(OpenclawTask task) {
        log.info("Triggering Openclaw async task | taskId: {}, name: {}", task.getId(), task.getName());

        try {
            // 1. 发起抓取请求 (现在是异步请求，不再阻塞等待结果)
            String crawlUrl = crawlerTriggerProperties.getOpenclawUrl();
            if (crawlUrl == null || crawlUrl.isBlank()) {
                log.warn("Openclaw crawl URL is not configured; skipping task [{}]", task.getId());
                return;
            }

            Map<String, Object> crawlBody = new HashMap<>();
            crawlBody.put("taskId", task.getId());
            crawlBody.put("name", task.getName());
            crawlBody.put("url", task.getUrl());
            crawlBody.put("crawlLogic", task.getCrawlLogic());
            crawlBody.put("interval", task.getConfig().getInterval());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> crawlEntity = new HttpEntity<>(crawlBody, headers);

            // 发起异步请求，Python 端会立即返回
            Map<String, Object> response = restTemplate.postForObject(crawlUrl, crawlEntity, Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                throw new DomainException("Failed to trigger async Openclaw crawl: " + response);
            }

            // 此处流程结束，剩余部分由 OpenclawCallbackJob 触达
            log.info("Openclaw async crawl task submitted | taskId: {}", task.getId());

        } catch (Exception e) {
            log.error("Error triggering Openclaw task | taskId: {}", task.getId(), e);
        }
    }

    @Transactional
    public String processCrawlResult(OpenclawTask task, List<Map<String, Object>> rawNews) {
        log.info("Processing Openclaw crawl results | taskId: {}", task.getId());
        StringBuilder contextBuilder = new StringBuilder();

        for (Map<String, Object> newsData : rawNews) {
            if (newsData == null) continue;

            String title = getIgnoreCase(newsData, "title", "name", "headline");
            String content = getIgnoreCase(newsData, "content", "summary", "abstract");
            String url = getIgnoreCase(newsData, "url", "link", "href");
            String source = getIgnoreCase(newsData, "source", "origin");

            if (title == null || title.isBlank()) title = "OpenClaw 自动抓取";
            if (url == null || url.isBlank()) url = task.getUrl();

            newsService.createNewsWithType(
                    task.getId(),
                    TaskType.OPENCLAW.getCode(),
                    title, content, url, source, LocalDateTime.now()
            );

            contextBuilder.append("Title: ").append(title).append("\nContent: ").append(content).append("\n---\n");
        }
        return contextBuilder.toString();
    }

    @Async
    public void triggerReportAsync(OpenclawTask task, String context) {
        log.info("Triggering Openclaw report async | taskId: {}", task.getId());
        String reportUrl = crawlerTriggerProperties.getOpenclawReportUrl();
        if (reportUrl == null || reportUrl.isBlank()) return;

        Map<String, Object> reportBody = new HashMap<>();
        reportBody.put("taskId", task.getId());
        reportBody.put("rawContent", context);
        reportBody.put("crawlLogic", task.getCrawlLogic());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> reportEntity = new HttpEntity<>(reportBody, headers);

        restTemplate.postForObject(reportUrl, reportEntity, Map.class);
    }

    @Transactional
    public void processReportResult(OpenclawTask task, String markdown) {
        log.info("Processing Openclaw report results | taskId: {}", task.getId());
        String cleanedMd = cleanMarkdown(markdown);
        Long reportId = reportService.createReportWithType(task.getId(), TaskType.OPENCLAW.getCode(), task.getName() + " 自动报表");
        reportService.completeReport(reportId, cleanedMd);
    }

    private String getIgnoreCase(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            // 精确匹配
            if (map.containsKey(key) && map.get(key) != null) return String.valueOf(map.get(key));
            // 忽略大小写寻找
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                    return String.valueOf(entry.getValue());
                }
            }
        }
        return null;
    }

    private String cleanMarkdown(String md) {
        if (md == null) return "";
        // Remove ```markdown ... ``` wrapper if present
        return md.replaceAll("(?s)^\\s*```markdown\\s*\\n?(.*?)\\n?\\s*```\\s*$", "$1").trim();
    }

    @Transactional
    public Long createOpenclawTask(Long userId, OpenclawTaskDTO.Create cmd) {
        OpenclawTask task = OpenclawTask.create(
                userId,
                cmd.getName(),
                cmd.getUrl(),
                cmd.getCrawlLogic(),
                cmd.getStartTime(),
                cmd.getEndTime(),
                cmd.getInterval()
        );
        return openclawTaskRepository.save(task);
    }

    @Transactional
    public void updateOpenclawTask(Long userId, Long taskId, OpenclawTaskDTO.Update cmd) {
        OpenclawTask task = openclawTaskRepository.findById(taskId);
        if (task == null) {
            throw new DomainException("Openclaw task not found");
        }
        task.validateOwnership(userId);
        task.update(cmd.getName(), cmd.getUrl(), cmd.getCrawlLogic(), cmd.getInterval());
        openclawTaskRepository.save(task);
    }

    @Transactional
    public TaskStatus switchOpenclawTask(Long userId, Long taskId, boolean enable) {
        OpenclawTask task = openclawTaskRepository.findById(taskId);
        if (task == null) {
            throw new DomainException("Openclaw task not found");
        }
        task.validateOwnership(userId);
        task.switchStatus(enable);
        openclawTaskRepository.save(task);
        return task.getStatus();
    }

    @Transactional
    public void deleteOpenclawTask(Long userId, Long taskId) {
        OpenclawTask task = openclawTaskRepository.findById(taskId);
        if (task == null) {
            throw new DomainException("Openclaw task not found");
        }
        task.validateOwnership(userId);
        openclawTaskRepository.delete(taskId);
    }

    public PageResult<OpenclawTaskVO> getOpenclawTasks(Long userId, int page, int size) {
        PageResult<OpenclawTask> result = openclawTaskRepository.findByUserId(userId, page, size);
        return new PageResult<>(
                result.getList().stream().map(this::toVO).collect(Collectors.toList()),
                result.getTotal()
        );
    }

    private OpenclawTaskVO toVO(OpenclawTask task) {
        return OpenclawTaskVO.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .name(task.getName())
                .url(task.getUrl())
                .crawlLogic(task.getCrawlLogic())
                .startTime(task.getConfig().getStartTime())
                .endTime(task.getConfig().getEndTime())
                .interval(task.getConfig().getInterval())
                .status(task.getStatus().getCode())
                .createTime(task.getCreateTime())
                .nextRunTime(task.getNextRunTime())
                .build();
    }
}
