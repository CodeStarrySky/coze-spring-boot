package com.wuch.coze.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

@RequiredArgsConstructor
public class RedisDataMemory implements DataMemory {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void add(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void clear(String key) {
        stringRedisTemplate.delete(key);
    }
}
