package com.kuafuai.common.dynamic_config.config;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@Deprecated
public class DynamicRefreshScope implements Scope {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        // 双重检查锁定模式确保线程安全
        Object bean = cache.get(name);
        if (bean == null) {
            synchronized (this.cache) {
                bean = cache.get(name);
                if (bean == null) {
                    bean = objectFactory.getObject();
                    cache.put(name, bean);
                }
            }
        }
        return bean;
    }

    @Override
    public Object remove(String name) {
        destructionCallbacks.remove(name);
        return cache.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        destructionCallbacks.put(name, callback);
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return "dynamicRefresh";
    }

    // 刷新特定bean
    public void refresh(String name) {
        synchronized (this.cache) {
            Object bean = cache.remove(name);
            if (bean != null) {
                Runnable callback = destructionCallbacks.get(name);
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    // 刷新所有bean
    public void refreshAll() {
        synchronized (this.cache) {
            cache.clear();
            destructionCallbacks.values().forEach(Runnable::run);
            destructionCallbacks.clear();
        }

    }
}
