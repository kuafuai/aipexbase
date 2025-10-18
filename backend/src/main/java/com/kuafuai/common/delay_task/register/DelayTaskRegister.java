package com.kuafuai.common.delay_task.register;

import com.kuafuai.common.delay_task.handler.AbstractDelayedTaskHandler;
import com.kuafuai.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
//@DependsOn(value = {"flywayInitializer"}) // 先执行flyway 然后再执行这个
//@ConditionalOnBean(name = "flywayInitializer")
public class DelayTaskRegister {

    private final Map<Class<?>, AbstractDelayedTaskHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, AbstractDelayedTaskHandler> handlersByClassName = new ConcurrentHashMap<>();

    public DelayTaskRegister() {

    }

    @PostConstruct
    public void init() {
        Collection<AbstractDelayedTaskHandler> beans = SpringUtils.getBeans(AbstractDelayedTaskHandler.class);

        beans.forEach(bean -> {
            handlers.put(bean.getSupportedType(), bean);
            handlersByClassName.put(bean.getSupportedType().getSimpleName(), bean);
        });

        log.info("Registered {} delay task handlers: {}", handlers.size(), handlers.keySet());
    }


    public void register(AbstractDelayedTaskHandler handler) {
        handlers.put(handler.getSupportedType(), handler);
        handlersByClassName.put(handler.getSupportedType().getSimpleName(), handler);
    }


    public AbstractDelayedTaskHandler getHandler(Class<?> clazz) {
        return handlers.get(clazz);
    }

    public AbstractDelayedTaskHandler getHandler(String clazzName) {
        return handlersByClassName.get(clazzName);
    }
}
