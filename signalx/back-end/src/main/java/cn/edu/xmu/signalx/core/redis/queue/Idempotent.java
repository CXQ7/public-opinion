package cn.edu.xmu.signalx.core.redis.queue;

import java.lang.annotation.*;

/**
 * 业务防重幂等注解
 * 用法：加在 record/report 的消费者方法上，自动防止 messageId 被重复执行
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** 幂等标记在 Redis 中的过期时间，默认 24 小时 */
    long expireSeconds() default 86400;

    /** 提示信息 */
    String message() default "该消息正在处理或已处理完成，请勿重复执行";
}
