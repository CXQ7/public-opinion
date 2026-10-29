package cn.edu.xmu.signalx.core.redis.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * 嵌入式 Redis 配置
 * 仅在测试环境下生效，自动在 6379 端口启动内存 Redis
 */
@TestConfiguration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        // 在内存中启动 Redis，避免依赖外部环境
        redisServer = new RedisServer(6379);
        try {
            redisServer.start();
            System.out.println("========================================");
            System.out.println("Embedded Redis 启动成功 | Port: 6379");
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("Embedded Redis 启动失败，可能 6379 端口已被占用: " + e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
            } catch (IOException e) {
                System.err.println("Embedded Redis 停止失败: " + e.getMessage());
            }
        }
    }
}
