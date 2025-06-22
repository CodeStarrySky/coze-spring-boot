package com.wuch.coze.autoconfiguration;

import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import com.wuch.coze.api.ChatClient;
import com.wuch.coze.api.CozeProperties;
import com.wuch.coze.auth.CozeAuth;
import com.wuch.coze.memory.DataMemory;
import com.wuch.coze.memory.DefaultDataMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnClass(CozeAPI.class)
@EnableConfigurationProperties({
        CozeProperties.class,})
@RequiredArgsConstructor
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class CozeAutoConfiguration {

    private final CozeProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public CozeAuth cozeAuth(DataMemory dataMemory) {
        if (CozeProperties.Auth.TYPE_JWT.equals(properties.getAuth().getType())) {
            return new com.wuch.coze.auth.JWTOAuth(properties);
        }
        return new com.wuch.coze.auth.TokenAuth(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CozeAPI cozeAPI(CozeAuth auth) {
        return  new CozeAPI.Builder()
                .baseURL(properties.getBaseUrl())
                .auth(auth.getAuth())
                .readTimeout(10000)
                .connectTimeout(10000)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @Order
    public ChatClient chatClient(CozeAPI cozeAPI, ApplicationContext applicationContext, DefaultListableBeanFactory beanFactory, DataMemory dataMemory) {
        return new ChatClient(applicationContext, cozeAPI, dataMemory, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @Order
    public DataMemory defaultDataMemory() {
        return new DefaultDataMemory();
    }
}



