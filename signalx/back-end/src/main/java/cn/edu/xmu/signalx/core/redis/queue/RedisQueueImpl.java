package cn.edu.xmu.signalx.core.redis.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 队列核心操作实现
 * 采用双 Key 模式：LIST 存储消息 ID，STRING 存储详细内容
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisQueueImpl implements RedisQueue {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeadLetterManager deadLetterManager;
    private final QueueProperties queueProperties;

    // 环境前缀，由配置或系统变量控制
    private final String envPrefix = "signalx:dev:";

    @Override
    public void push(QueueMessage<?> message) {
        String queueKey = envPrefix + "queue:" + message.getTopic();
        String msgKey = envPrefix + "msg:" + message.getMessageId();

        try {
            // 1. 存储详情：存入消息详细内容，设置 7 天有效期
            redisTemplate.opsForValue().set(msgKey, message, Duration.ofDays(7));

            // 2. 入队：将 ID 推入 Redis List 末尾
            redisTemplate.opsForList().rightPush(queueKey, message.getMessageId());

            log.debug("消息已入队 | topic: {}, msgId: {}", message.getTopic(), message.getMessageId());
        } catch (Exception e) {
            log.error("消息推送失败 | topic: {}", message.getTopic(), e);
            throw new RuntimeException("Redis 队列写入异常", e);
        }
    }

    @Override
    public Optional<QueueMessage<?>> pop(String topic) {
        String queueKey = envPrefix + "queue:" + topic;

        // 1. 弹出消息 ID
        Object messageId = redisTemplate.opsForList().leftPop(queueKey);
        if (messageId == null) return Optional.empty();

        // 2. 根据 ID 获取内容详情
        String msgKey = envPrefix + "msg:" + messageId;
        QueueMessage<?> message = (QueueMessage<?>) redisTemplate.opsForValue().get(msgKey);

        return Optional.ofNullable(message);
    }

    @Override
    public void ack(String messageId) {
        // 处理成功，删除详情池中的内容副本
        String msgKey = envPrefix + "msg:" + messageId;
        redisTemplate.delete(msgKey);
        log.debug("消息已签收并清理详情 | msgId: {}", messageId);
    }

    @Override
    public void nack(QueueMessage<?> message, Throwable e) {
        int maxRetry = message.getMaxRetry() > 0 ? message.getMaxRetry() : queueProperties.getDefaultMaxRetry();

        // 先自增计数，再判断是否超限（语义：maxRetry=3 表示最多重试 3 次）
        message.setRetryCount(message.getRetryCount() + 1);

        if (message.getRetryCount() < maxRetry) {
            log.warn("准备进行第 {} 次重试 | msgId: {}, topic: {}",
                    message.getRetryCount(), message.getMessageId(), message.getTopic());
            this.push(message);
        } else {
            // 超过重试次数，转移至死信队列
            deadLetterManager.moveToDeadLetter(message, e);
        }
    }
}
