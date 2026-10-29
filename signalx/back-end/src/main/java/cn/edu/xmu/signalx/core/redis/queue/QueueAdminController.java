package cn.edu.xmu.signalx.core.redis.queue;

import cn.edu.xmu.signalx.core.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 队列基础运维接口 (Admin 调用)
 * 功能：查看统计积压、一键重放死信
 */
@RestController
@RequestMapping("/api/v1/admin/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueAdminController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisQueue redisQueue;
    private final String envPrefix = "signalx:dev:";

    /**
     * 统计指定 Topic 的死信积压情况
     * @param topic 队列主题
     * @return 死信数量
     */
    @GetMapping("/dlq/stats")
    public Result<Long> getDlqStats(@RequestParam String topic) {
        String dlqKey = envPrefix + "dlq:" + topic;
        Long size = redisTemplate.opsForList().size(dlqKey);
        return Result.success(size != null ? size : 0L);
    }

    /**
     * 手动触发将死信重回主队列流程
     * @param topic 需要重放的分类频道
     */
    @PostMapping("/dlq/replay")
    public Result<String> replayDeadLetter(@RequestParam String topic) {
        String dlqKey = envPrefix + "dlq:" + topic;
        long count = 0;

        while (true) {
            Object msgObj = redisTemplate.opsForList().leftPop(dlqKey);
            if (msgObj == null) break;

            if (msgObj instanceof QueueMessage) {
                QueueMessage<?> message = (QueueMessage<?>) msgObj;
                // 重置重试次数后重新入队
                message.setRetryCount(0);
                redisQueue.push(message);
                count++;
            }
        }

        log.info("手动重发死信完成 | topic: {}, 数量: {}", topic, count);
        return Result.success(String.format("手动重播已完成，共处理 %d 个死信消息", count));
    }
}
