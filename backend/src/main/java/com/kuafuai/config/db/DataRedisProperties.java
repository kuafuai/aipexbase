package com.kuafuai.config.db;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConditionalOnProperty(name = "app.db.enable", havingValue = "true")
public class DataRedisProperties {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.database}")
    private Integer database;

    @Value("${spring.redis.port}")
    private Integer port;

    @Value("${spring.redis.ssl}")
    private Boolean ssl;
}
