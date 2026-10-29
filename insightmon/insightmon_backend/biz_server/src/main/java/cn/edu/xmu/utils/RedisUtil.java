package cn.edu.xmu.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUtil {
    private final RedisTemplate redisTemplate;
    public final long DEFAULT_TIMEOUT = 600;

    public Future<Void> set(String key, Object value, long timeout) {
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            return null;
        });
        new Thread(futureTask).start();
        return futureTask;
    }

    public Future<Void> set(String key, Object value) {
        return set(key, value, DEFAULT_TIMEOUT);
    }

    public Object get(String key){
        return redisTemplate.opsForValue().get(key);
    }
}
