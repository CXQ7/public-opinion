package cn.edu.xmu.signalx.core.redis.queue;

import cn.edu.xmu.signalx.SignalXApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 可靠性测试：验证重试机制与死信队列(DLQ)
 * 验证：Nack 触发计数增加、达到阈值后转入 DLQ
 */
@SpringBootTest(classes = SignalXApplication.class)
public class QueueReliabilityTest {

    @Autowired
    private RedisQueue redisQueue;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("测试消息重试及死信队列投递链路")
    void testNackAndDLQ() {
        String topic = "reliability.topic";
        String payload = "Reliability Test Message";
        QueueMessage<String> message = QueueMessage.create(topic, payload);
        String msgId = message.getMessageId();

        // 基础环境准备
        String envPrefix = "signalx:dev:";
        String queueKey = envPrefix + "queue:" + topic;
        String dlqKey = envPrefix + "dlq:" + topic;
        // 清理所有相关 Key，避免上次测试残留数据干扰
        redisTemplate.delete(queueKey);
        redisTemplate.delete(dlqKey);

        // 1. 入队
        redisQueue.push(message);

        // 2. 模拟消费失败 (Nack)
        // 第一次失败：应当重新回到队列头部，重试计数从 0 变 1
        redisQueue.nack(message, new RuntimeException("First Fail"));

        // 3. 再次弹出验证重试计数
        QueueMessage<?> retryMessage = redisQueue.pop(topic).orElseThrow();
        Assertions.assertEquals(1, retryMessage.getRetryCount(), "初次重试后计数应当递增");

        // 4. 重复 Nack 直至超过限制 (3次)
        // 假设配置中的 maxRetries 是 3 (注：此处取决于 RedisQueueImpl 中的逻辑判定)
        redisQueue.nack(retryMessage, new RuntimeException("Second Fail"));
        QueueMessage<?> retryMessage2 = redisQueue.pop(topic).orElseThrow();
        redisQueue.nack(retryMessage2, new RuntimeException("Third Fail"));

        // 5. 验证是否自动被投递转移到死信队列 (DLQ)
        // 系统应当识别到 retryCount >= maxRetries，将其从 List 移除，存入 dlqKey
        Long dlqSize = redisTemplate.opsForList().size(dlqKey);
        Assertions.assertTrue(dlqSize != null && dlqSize > 0, "三次重试失败后应当进入死信队列");

        // 6. 验证详情池是否仍存续 (DLQ 依然需要 Payload 以便将来重试/对比)
        String msgKey = envPrefix + "msg:" + msgId;
        Assertions.assertNotNull(redisTemplate.opsForValue().get(msgKey), "进入死信队列后详情池数据不应被删除");

        // 7. 测试管理员手动 Replay 清理 DLQ (可选)
        // ... 此处可扩展验证 controller 中调用的 replay 逻辑
    }
}
