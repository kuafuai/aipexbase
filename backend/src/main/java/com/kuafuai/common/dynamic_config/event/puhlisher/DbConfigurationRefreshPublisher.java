package com.kuafuai.common.dynamic_config.event.puhlisher;

import com.kuafuai.common.dynamic_config.event.DbConfigurationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DbConfigurationRefreshPublisher {
    @Resource
    private ApplicationContext applicationContext;


    public void publishEvent(String propertyName, Object propertyValue) {
        applicationContext.publishEvent(new DbConfigurationEvent(this, propertyName, propertyValue));
    }
}
