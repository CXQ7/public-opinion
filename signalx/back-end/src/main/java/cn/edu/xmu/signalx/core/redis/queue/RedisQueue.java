package cn.edu.xmu.signalx.core.redis.queue;

import java.util.Optional;

/**
 * Redis 队列核心操作契约
 * 规范：定义了入队、出队、确认和异常处理的标准动作
 */
public interface RedisQueue {

    /**
     * 入队：将消息推入队列
     * @param message 规范的消息控制体
     */
    void push(QueueMessage<?> message);

    /**
     * 出队：从队列中获取一条待处理消息
     * @param topic 队列主题
     * @return 消息体，若无则返回 Empty
     */
    Optional<QueueMessage<?>> pop(String topic);

    /**
     * 确认成功：业务处理完成后调用，清理中间状态
     * @param messageId 消息唯一ID
     */
    void ack(String messageId);

    /**
     * 确认失败：业务异常时调用，触发重试或进入死信队列
     * @param message 消息体
     * @param e 异常原因
     */
    void nack(QueueMessage<?> message, Throwable e);
}
