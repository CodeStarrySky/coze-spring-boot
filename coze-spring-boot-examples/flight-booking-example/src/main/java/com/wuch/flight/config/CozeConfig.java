package com.wuch.flight.config;

import com.wuch.coze.memory.DataMemory;
import com.wuch.coze.memory.RedisDataMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CozeConfig {

  //  @Bean
//    public DataMemory redisDataMemory(StringRedisTemplate stringRedisTemplate) {
//        return new RedisDataMemory(stringRedisTemplate);
//    }


}
