package cn.edu.xmu.signalx.core.redis.queue;

import cn.edu.xmu.signalx.SignalXApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

/**
 * Redis 队列基础逻辑单元测试
 * 验证：入队、出队、双 Key 模式一致性、Ack 销毁逻辑
 */
@SpringBootTest(classes = SignalXApplication.class)
public class RedisQueueTest {

    @Autowired
    private RedisQueue redisQueue;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("测试消息入队与出队完整流程")
    void testPushAndPop() {
        String topic = "test.topic";
        String payload = "Hello SignalX Redis Queue";
        QueueMessage<String> message = QueueMessage.create(topic, payload);
        String msgId = message.getMessageId();

        // 1. 入队
        redisQueue.push(message);

        // 2. 验证双 Key 是否存在
        String envPrefix = "signalx:dev:"; // 当前代码中硬编码的值
        String queueKey = envPrefix + "queue:" + topic;
        String msgKey = envPrefix + "msg:" + msgId;

        // 检查 List 中是否有 ID
        Long size = redisTemplate.opsForList().size(queueKey);
        Assertions.assertTrue(size != null && size > 0, "队列中应存在 ID");

        // 检查详情池中是否有内容
        Object cachedMsg = redisTemplate.opsForValue().get(msgKey);
        Assertions.assertNotNull(cachedMsg, "消息详情池中应存在内容");

        // 3. 出队
        Optional<QueueMessage<?>> poppable = redisQueue.pop(topic);
        Assertions.assertTrue(poppable.isPresent(), "应当能弹出消息");

        QueueMessage<?> poppedMessage = poppable.get();
        Assertions.assertEquals(msgId, poppedMessage.getMessageId(), "弹出的消息ID不匹配");
        Assertions.assertEquals(payload, poppedMessage.getPayload(), "消息载荷(Payload)损坏");

        // 4. 验证出队后详情仍然保留（等待 Ack）
        Assertions.assertNotNull(redisTemplate.opsForValue().get(msgKey), "出队后未 Ack 前内容不应被删除");

        // 5. 确认成功 (Ack)
        redisQueue.ack(poppedMessage.getMessageId());
        Assertions.assertNull(redisTemplate.opsForValue().get(msgKey), "Ack 后内容应当被物理清理");

        // 清理测试数据 (LLEN 为 0 时不用特意清理 Key)
    }
}
