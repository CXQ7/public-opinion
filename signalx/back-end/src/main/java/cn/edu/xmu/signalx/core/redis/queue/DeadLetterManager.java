package cn.edu.xmu.signalx.core.redis.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 死信管理：负责将超过最大重试次数的消息转移至死信队列
 * 特性：自动触发带有 [DLQ-ALARM] 标签的异常日志
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String envPrefix = "signalx:dev:";

    /**
     * 转移消息至死信队列
     * @param message 消息控制体
     * @param e 最终导致失败的异常原因
     */
    public void moveToDeadLetter(QueueMessage<?> message, Throwable e) {
        // 拼装死信 Key: signalx:dlq:{topic}
        String dlqKey = envPrefix + "dlq:" + message.getTopic();

        log.error("[DLQ-ALARM] 消息处理失败且超过重试限制，已移入死信队列: {}, messageId: {}, traceId: {}, 异常原因: {}",
                dlqKey, message.getMessageId(), message.getTraceId(), e.getMessage());

        // 实际物理操作：将消息推入死信队列存储
        try {
            redisTemplate.opsForList().rightPush(dlqKey, message);
        } catch (Exception ex) {
            log.error("写入死信队列失败 | messageId: {}", message.getMessageId(), ex);
        }
    }
}
