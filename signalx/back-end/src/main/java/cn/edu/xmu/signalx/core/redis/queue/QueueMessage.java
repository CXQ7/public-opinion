package cn.edu.xmu.signalx.core.redis.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 通用 Redis 队列消息协议体
 * 规范：record 和 report 模块往 core 发送数据时必须封装在此类中
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessage<T> implements Serializable {

    /** 消息唯一ID，用于防重(幂等)和追踪 */
    private String messageId;

    /** 消息主题，对应 Redis 中的 Key 分类 */
    private String topic;

    /** 业务核心数据 (如 taskId, reportId 等) */
    private T payload;

    /** 当前已重试次数 */
    private int retryCount;

    /** 最大允许重试次数 */
    private int maxRetry;

    /** 消息生成时间 */
    private LocalDateTime timestamp;

    /** 链路追踪ID，用于日志关联 */
    private String traceId;

    public static <T> QueueMessage<T> create(String topic, T payload) {
        return QueueMessage.<T>builder()
                .messageId(UUID.randomUUID().toString())
                .topic(topic)
                .payload(payload)
                .retryCount(0)
                .maxRetry(3)
                .timestamp(LocalDateTime.now())
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .build();
    }
}
