package com.wuch.coze.autoconfiguration;

import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import com.wuch.coze.api.ChatClient;
import com.wuch.coze.api.CozeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnClass(CozeAPI.class)
@EnableConfigurationProperties(CozeProperties.class)
@RequiredArgsConstructor
public class CozeAutoConfiguration {

    private final CozeProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public CozeAPI cozeAPI() {
        TokenAuth authCli = new TokenAuth(properties.getToken());
        return  new CozeAPI.Builder()
                .baseURL(properties.getBaseUrl())
                .auth(authCli)
                .readTimeout(10000)
                .connectTimeout(10000)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(CozeAPI cozeAPI, ApplicationContext applicationContext, StringRedisTemplate stringRedisTemplate) {
        return new ChatClient(applicationContext, cozeAPI, stringRedisTemplate, properties);
    }

}



