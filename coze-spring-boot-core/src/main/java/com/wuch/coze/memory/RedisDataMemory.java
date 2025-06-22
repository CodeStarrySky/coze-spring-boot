package com.wuch.coze.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;


@RequiredArgsConstructor
public class RedisDataMemory implements DataMemory {

    private final StringRedisTemplate stringRedisTemplate;

//    private final RedissonClient redissonClient;

    @Override
    public void add(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void add(String key, String hashKey, String value) {
        stringRedisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public String get(String key, String hashKey) {
        Object value = stringRedisTemplate.opsForHash().get(key, hashKey);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    @Override
    public void clear(String key) {
        stringRedisTemplate.delete(key);
    }
}
