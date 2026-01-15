package com.kuafuai.config.db;

import com.kuafuai.common.util.StringUtils;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ConditionalOnProperty(name = "app.db.enable", havingValue = "true")
@Slf4j
public class DataSourceConfig {

    @Resource
    private DataRedisProperties dataRedisProperties;

    @Bean
    @Primary
    public DynamicRoutingDataSource dataSource() {
        DynamicRoutingDataSource ds = new DynamicRoutingDataSource();
        ds.setTargetDataSources(new ConcurrentHashMap<>());
        ds.afterPropertiesSet();
        return ds;
    }

    public LettuceConnectionFactory redisConnectionFactory() {

        ClientResources clientResources = DefaultClientResources.create();
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .build();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(dataRedisProperties.getHost());

        if (dataRedisProperties.getPort() != null) {
            config.setPort(dataRedisProperties.getPort());
        } else {
            config.setPort(6379);
        }
        if (dataRedisProperties.getDatabase() != null) {
            config.setDatabase(dataRedisProperties.getDatabase());
        } else {
            config.setDatabase(0);
        }

        if (StringUtils.isNotEmpty(dataRedisProperties.getPassword())) {
            config.setPassword(dataRedisProperties.getPassword());
        }

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean("dataRouterRedisTemplate")
    public RedisTemplate<String, Object> bizRedisTemplate() {
        LettuceConnectionFactory dataRouterRedisConnection = redisConnectionFactory();
        dataRouterRedisConnection.afterPropertiesSet();

        return buildTemplate(dataRouterRedisConnection);
    }

    private RedisTemplate<String, Object> buildTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }


}
