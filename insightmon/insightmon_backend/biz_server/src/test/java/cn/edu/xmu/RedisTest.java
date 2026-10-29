package cn.edu.xmu;

import cn.edu.xmu.domain.po.User;
import cn.edu.xmu.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void getNum() {
        System.out.println(redisTemplate.opsForValue().get("num"));
    }

    @Test
    void setAndGet() {
        redisTemplate.opsForValue().set("name", "李四");
        System.out.println(redisTemplate.opsForValue().get("name"));
    }

    @Test
    void setAndGetObject() {
        User user = new User();
        user.setId(1L);
        user.setPassword("123456");
        user.setUsername("alice");
        user.setStatus(UserStatus.NORMAL);

        redisTemplate.opsForValue().set("userKey", user, 10, TimeUnit.SECONDS);
        // 从 Redis 中取出对象
        User retrievedUser = (User) redisTemplate.opsForValue().get("userKey");

        System.out.println("Retrieved user from Redis: " + retrievedUser);
    }
}
