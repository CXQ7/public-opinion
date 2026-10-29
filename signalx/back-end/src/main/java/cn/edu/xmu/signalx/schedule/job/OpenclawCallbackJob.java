package cn.edu.xmu.signalx.schedule.job;

import cn.edu.xmu.signalx.core.common.TaskType;
import cn.edu.xmu.signalx.core.redis.queue.QueueMessage;
import cn.edu.xmu.signalx.core.redis.queue.RedisQueue;
import cn.edu.xmu.signalx.record.service.NewsService;
import cn.edu.xmu.signalx.report.service.ReportService;
import cn.edu.xmu.signalx.task.domain.model.OpenclawTask;
import cn.edu.xmu.signalx.task.domain.repository.OpenclawTaskRepository;
import cn.edu.xmu.signalx.task.service.OpenclawTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Openclaw 异步结果结果监听器
 * 负责从 Redis 队列中消费 Python 爬虫产生的回馈
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OpenclawCallbackJob {

    private final RedisQueue redisQueue;
    private final OpenclawTaskService openclawTaskService;
    private final OpenclawTaskRepository openclawTaskRepository;
    private final NewsService newsService;
    private final ReportService reportService;

    @Scheduled(fixedDelay = 5000) // 每5秒轮询一次结果队列
    public void listenCallback() {
        while (true) {
            Optional<QueueMessage<?>> msgOpt = redisQueue.pop("openclaw.callback");
            if (msgOpt.isEmpty()) {
                break;
            }

            QueueMessage<?> message = msgOpt.get();
            try {
                handleMessage(message);
                redisQueue.ack(message.getMessageId());
            } catch (Exception e) {
                log.error("处理 Openclaw 回调失败 | msgId: {}", message.getMessageId(), e);
                redisQueue.nack(message, e);
            }
        }
    }

    private void handleMessage(QueueMessage<?> message) {
        // 由于 Python 推送的是 JSON 字符串形式的 payload 以规避 Jackson 校验
        // 我们需要先将其解析为 Map
        String rawPayload = (String) message.getPayload();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> payload;
        try {
            payload = mapper.readValue(rawPayload, Map.class);
        } catch (Exception e) {
            log.error("解析 Openclaw 原始 Payload 失败 | msgId: {}", message.getMessageId(), e);
            return;
        }

        Long taskId = Long.valueOf(payload.get("taskId").toString());
        String type = (String) payload.get("type");
        Object data = payload.get("data");

        // 增加详细的调试日志，打印接收到的完整 data 内容
        log.info("接收到 Openclaw 回调消息 | taskId: {}, type: {}", taskId, type);
        if (log.isDebugEnabled()) {
            log.debug("回调数据内容为: {}", data);
        } else {
            // 如果没开 DEBUG 级别，也可以用 INFO 打印截断的内容，方便快速查看
            String dataStr = String.valueOf(data);
            log.info("回调数据内容览: {}", dataStr.length() > 500 ? dataStr.substring(0, 500) + "..." : dataStr);
        }

        OpenclawTask task = openclawTaskRepository.findById(taskId);
        if (task == null) {
            log.warn("回调对应的任务不存在 | taskId: {}", taskId);
            return;
        }

        if ("CRAWL_RESULT".equalsIgnoreCase(type)) {
            // 处理抓取结果回调
            List<Map<String, Object>> rawNews = (List<Map<String, Object>>) data;

            // 1. 持久化 News 并生成 context
            String context = openclawTaskService.processCrawlResult(task, rawNews);

            // 2. 触发报表生成请求 (由于是异步回调，这里再次发起报表生成请求)
            openclawTaskService.triggerReportAsync(task, context);

        } else if ("REPORT_RESULT".equalsIgnoreCase(type)) {
            // 处理报表生成回调
            String markdown = (String) data;

            // 1. 完成报表生成流程
            openclawTaskService.processReportResult(task, markdown);

            // 2. 推进调度
            task.advanceSchedule();
            openclawTaskRepository.save(task);
            log.info("Openclaw 全链路任务完成 | taskId: {}", task.getId());
        }
    }
}
