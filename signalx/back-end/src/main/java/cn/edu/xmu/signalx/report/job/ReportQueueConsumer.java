package cn.edu.xmu.signalx.report.job;

import cn.edu.xmu.signalx.core.redis.queue.Idempotent;
import cn.edu.xmu.signalx.core.redis.queue.QueueMessage;
import cn.edu.xmu.signalx.core.redis.queue.RedisQueue;
import cn.edu.xmu.signalx.report.domain.model.Report;
import cn.edu.xmu.signalx.report.domain.model.ReportStatus;
import cn.edu.xmu.signalx.report.domain.repository.ReportRepository;
import cn.edu.xmu.signalx.report.infrastructure.client.CozeClient;
import cn.edu.xmu.signalx.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Report 模块 Redis 队列消费者
 * 处理 report.poll 队列中的消息，统一轮询 Coze 任务状态
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportQueueConsumer {

    private final RedisQueue redisQueue;
    private final ReportRepository reportRepository;
    private final CozeClient reportClient;
    private final ReportService reportService;

    /**
     * 消费 report.poll 队列，每秒轮询一次
     */
    @Scheduled(fixedDelay = 1000)
    public void consumePollTask() {
        Optional<QueueMessage<?>> optional = redisQueue.pop("report.poll");
        if (optional.isEmpty()) return;

        QueueMessage<?> msg = optional.get();
        try {
            processPollTask(msg);
            redisQueue.ack(msg.getMessageId());
        } catch (Exception e) {
            log.error("处理轮询任务失败 | msgId: {}", msg.getMessageId(), e);
            redisQueue.nack(msg, e);
        }
    }

    /**
     * 处理轮询任务
     * @param message 队列消息
     */
    @Idempotent(expireSeconds = 3600)
    public void processPollTask(QueueMessage<?> message) {
        Long reportId = (Long) message.getPayload();
        Report report = reportRepository.findById(reportId);
        if (report == null) {
            log.error("报表不存在，跳过轮询 | reportId={}", reportId);
            return;
        }

        if (report.getCozeRunId() == null) {
            log.error("报表缺少 CozeRunId，跳过轮询 | reportId={}", reportId);
            return;
        }

        // 查询 Coze 任务状态
        CozeClient.CozeResult result = reportClient.query(report.getCozeRunId());
        if (result.isCompleted()) {
            log.info("Coze 任务完成 | reportId={} | 报表内容长度={}", reportId, result.getMarkdown().length());
            reportService.processCompletedReport(reportId, result.getMarkdown());
        } else if (result.isFailed()) {
            log.error("Coze 任务失败 | reportId={} | runId={}", reportId, report.getCozeRunId());
            updateReportStatus(reportId, ReportStatus.FAILED, null);
        } else {
            // 任务未完成，重新入队等待下一次轮询
            QueueMessage<Long> newMessage = QueueMessage.create("report.poll", reportId);
            redisQueue.push(newMessage);
            log.debug("Coze 任务处理中，重新入队 | reportId={}", reportId);
        }
    }

    /**
     * 更新报表状态
     * @param reportId 报表 ID
     * @param status 目标状态
     * @param fileUrl COS 文件地址（可选）
     */
    private void updateReportStatus(Long reportId, ReportStatus status, String fileUrl) {
        Report report = reportRepository.findById(reportId);
        if (report == null) return;

        if (status == ReportStatus.FAILED) {
            report.fail();
            log.info("报表标记为失败 | reportId={}", reportId);
        }
        if (fileUrl != null) {
            report.setFileUrl(fileUrl);
        }
        reportRepository.save(report);
    }
}
