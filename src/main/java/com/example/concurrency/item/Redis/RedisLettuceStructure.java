package com.example.concurrency.item.Redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisLettuceStructure {

    private final RedisTemplate<String, String> redisTemplate;

    public Boolean generateLock(final Long key, int timeout) {
        return redisTemplate
                .opsForValue()
                //setnx 명령어 - key(key) value("lock")
                .setIfAbsent(key.toString(), "lock", Duration.ofMillis(timeout));
    }

    public Boolean deleteLock(final Long key) {
        return redisTemplate.delete(key.toString());
    }
}