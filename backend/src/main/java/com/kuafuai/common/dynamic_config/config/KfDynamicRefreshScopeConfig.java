package com.kuafuai.common.dynamic_config.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//@Deprecated
@Configuration
public class KfDynamicRefreshScopeConfig {

    @Bean
    public  BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return beanFactory -> {
            // 注册自定义作用域
            beanFactory.registerScope("dynamicRefresh", dynamicRefreshScopeConfig());
        };
    }

    @Bean
    public  DynamicRefreshScope dynamicRefreshScopeConfig() {
        return new DynamicRefreshScope();
    }

}
