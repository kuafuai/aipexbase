package com.kuafuai.config;

import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "cache.type", havingValue = "local" )
@EnableAutoConfiguration(exclude = {RedissonAutoConfiguration.class, RedisAutoConfiguration.class})
public class ExcludeRedissonConfig {
}
