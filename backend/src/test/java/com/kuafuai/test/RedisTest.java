package com.kuafuai.test;

import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
public class RedisTest {

    private RedisTemplate redisTemplate() {

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientResources(DefaultClientResources.create())
                .useSsl()
                .disablePeerVerification()
                .build();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("redis-7kmkww.serverless.euc1.cache.amazonaws.com");
//        config.setPort(6279);
//        config.setDatabase(2);

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config, clientConfig);
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Test
    public void test_set() {
        RedisTemplate redisTemplate = redisTemplate();
        Object value = redisTemplate.opsForValue().get("codeflying:user_info:8567");
        log.info("{}", value);
    }
}
