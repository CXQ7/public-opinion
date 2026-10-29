package cn.edu.xmu.signalx.core.redis.queue;

import cn.edu.xmu.signalx.SignalXApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 幂等切面集成测试
 * 验证：@Idempotent 注解是否能有效拦截重复请求
 */
@SpringBootTest(classes = {SignalXApplication.class, IdempotentAspectTest.TestOrderService.class})
@Slf4j
public class IdempotentAspectTest {

    @Autowired
    private TestOrderService testOrderService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("测试幂等拦截器：同消息ID只能执行一次")
    void testIdempotentExecution() {
        String msgId = "test-idempotent-001";
        QueueMessage<String> message = new QueueMessage<>();
        message.setMessageId(msgId);

        // 清理脏数据
        redisTemplate.delete("signalx:idempotent:" + msgId);
        testOrderService.resetCounter();

        // 1. 第一次执行：应当成功
        testOrderService.processTask(message);
        Assertions.assertEquals(1, testOrderService.getCounter(), "第一次处理应当成功增加计数");

        // 2. 第二次执行：应当被拦截（计数器不该增加）
        testOrderService.processTask(message);
        Assertions.assertEquals(1, testOrderService.getCounter(), "第二次相同消息 ID 应当被拦截");

        // 3. 验证过期/删除后的重试
        redisTemplate.delete("signalx:idempotent:" + msgId);
        testOrderService.processTask(message);
        Assertions.assertEquals(2, testOrderService.getCounter(), "手动删除幂等标记后应当允许再次执行");
    }

    /**
     * 辅助测试类：模拟业务逻辑
     */
    @Component
    public static class TestOrderService {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Idempotent(expireSeconds = 10)
        public void processTask(QueueMessage<String> message) {
            log.info("模拟业务处理中 | msgId: {}", message.getMessageId());
            counter.incrementAndGet();
        }

        public int getCounter() { return counter.get(); }
        public void resetCounter() { counter.set(0); }
    }
}
