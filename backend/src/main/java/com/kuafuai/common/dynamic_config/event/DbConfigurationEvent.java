package com.kuafuai.common.dynamic_config.event;

import org.springframework.context.ApplicationEvent;

/**
 * 数据库配置的修改类
 */
public class DbConfigurationEvent extends ApplicationEvent {
    private String propertyName;

    private Object propertyValue;

    public DbConfigurationEvent(Object source, String propertyName, Object propertyValue) {
        super(source);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    @Override
    public String toString() {
        return "DbConfigurationEvent{" +
                "propertyName='" + propertyName + '\'' +
                ", propertyValue=" + propertyValue +
                '}';
    }
}
