package cn.edu.xmu.signalx.report.job;

import cn.edu.xmu.signalx.core.redis.queue.Idempotent;
import cn.edu.xmu.signalx.core.redis.queue.QueueMessage;
import cn.edu.xmu.signalx.core.redis.queue.RedisQueue;
import cn.edu.xmu.signalx.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Report 模块 Redis 队列消费者
 * 处理 report.generate 队列中的消息，触发报表生成
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportGenerateConsumer {

    private final RedisQueue redisQueue;
    private final ReportService reportService;

    /**
     * 消费 report.generate 队列，每秒轮询一次
     */
    @Scheduled(fixedDelay = 1000)
    public void consumeGenerateTask() {
        Optional<QueueMessage<?>> optional = redisQueue.pop("report.generate");
        if (optional.isEmpty()) return;

        QueueMessage<?> msg = optional.get();
        try {
            processGenerateTask(msg);
            redisQueue.ack(msg.getMessageId());
        } catch (Exception e) {
            log.error("处理报表生成任务失败 | msgId: {}", msg.getMessageId(), e);
            redisQueue.nack(msg, e);
        }
    }

    /**
     * 处理报表生成任务
     * @param message 队列消息
     */
    @Idempotent(expireSeconds = 3600)
    public void processGenerateTask(QueueMessage<?> message) {
        Long taskId = (Long) message.getPayload();
        log.info("开始处理报表生成任务 | taskId={}", taskId);
        reportService.triggerReportForTask(taskId, 0);
    }
}
