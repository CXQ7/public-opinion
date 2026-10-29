package cn.edu.xmu.signalx.core.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 极简 Redis 连接连通性测试
 */
public class RedisConnectivityTest {

    @Test
    void testConnection() {
        try {
            // 尝试连接本地默认 6379 端口
            LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
            factory.afterPropertiesSet();

            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.opsForValue().set("signalx_connection_test", "SUCCESS");

            String result = template.opsForValue().get("signalx_connection_test");
            System.out.println("========================================");
            System.out.println("Redis 连接测试结果: " + result);
            System.out.println("========================================");

            factory.destroy();
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("Redis 连接失败！原因: " + e.getMessage());
            System.err.println("请确保本地 redis-server.exe 正在运行。");
            System.err.println("========================================");
            throw e;
        }
    }
}
