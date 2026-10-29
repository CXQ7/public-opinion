package cn.edu.xmu.signalx.record.job;

import cn.edu.xmu.signalx.core.redis.queue.Idempotent;
import cn.edu.xmu.signalx.core.redis.queue.QueueMessage;
import cn.edu.xmu.signalx.core.redis.queue.RedisQueue;
import cn.edu.xmu.signalx.record.port.CrawlerRunner;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewsCrawlConsumer {

    private final RedisQueue redisQueue;
    private final TaskRepository taskRepository;
    private final CrawlerRunner crawlerRunner;

    @Scheduled(fixedDelay = 500)
    public void consumeNewsCrawlTask() {
        Optional<QueueMessage<?>> optional = redisQueue.pop("news.crawl");
        if (optional.isEmpty()) return;

        QueueMessage<?> msg = optional.get();
        try {
            processCrawlTask((QueueMessage<Long>) msg);
            redisQueue.ack(msg.getMessageId());
        } catch (Exception e) {
            log.error("News crawl failed | msgId: {}", msg.getMessageId(), e);
            redisQueue.nack(msg, e);
        }
    }

    @Idempotent(expireSeconds = 300)
    public void processCrawlTask(QueueMessage<Long> message) {
        Long taskId = message.getPayload();

        Task task = taskRepository.findById(taskId);
        if (task == null) {
            log.warn("Task not found for crawler execution | taskId: {}", taskId);
            return;
        }

        crawlerRunner.run(task);
    }
}
