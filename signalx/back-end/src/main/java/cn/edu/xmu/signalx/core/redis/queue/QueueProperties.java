package cn.edu.xmu.signalx.core.redis.queue;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 队列配置项映射类
 * 关联 application.yml 中的 signalx.queue 前缀
 */
@Data
@Component
@ConfigurationProperties(prefix = "signalx.queue")
public class QueueProperties {

    /** 默认最大重试次数 */
    private int defaultMaxRetry = 3;

    /** 幂等标记默认保留时长 (秒) */
    private long idempotentTtl = 86400;

    /** 重试间隔基础值 (秒) */
    private int retryInterval = 5;

    /** 开启死信报警日志 */
    private boolean enableDlqAlert = true;
}
