package com.kuafuai.common.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EventBusConfig {

    @Bean(name = "eventTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("eventbus-");
        executor.initialize();
        return executor;
    }

    @Bean("myAsyncEventBus")
    public EventBus createAsyncEventBus(@Qualifier("eventTaskExecutor") ThreadPoolTaskExecutor threadPool) {
        return new AsyncEventBus(threadPool);
    }
}
