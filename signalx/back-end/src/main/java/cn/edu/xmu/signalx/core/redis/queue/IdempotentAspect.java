package cn.edu.xmu.signalx.core.redis.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 幂等切面实现
 * 逻辑：执行业务前先去 Redis 占位，占不到说明重复
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String idempotentPrefix = "signalx:idempotent:";

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 从参数中提取第一个符合 QueueMessage 类型的参数 (作为 Key)
        Object[] args = joinPoint.getArgs();
        String messageId = null;

        for (Object arg : args) {
            if (arg instanceof QueueMessage) {
                messageId = ((QueueMessage<?>) arg).getMessageId();
                break;
            }
        }

        if (messageId == null) {
            log.warn("无法从方法参数中提取 messageId，幂等保护失效！");
            return joinPoint.proceed();
        }

        String key = idempotentPrefix + messageId;

        // 分布式占位 (SETNX)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1",
                Duration.ofSeconds(idempotent.expireSeconds()));

        if (Boolean.TRUE.equals(success)) {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                // 失败了移除标记，允许下一次重试尝试
                redisTemplate.delete(key);
                throw throwable;
            }
        } else {
            log.warn("[IDEMPOTENT] 发现重复处理，已拦截并跳过 | msgId: {}", messageId);
            return null; // 直接跳过业务执行
        }
    }
}
