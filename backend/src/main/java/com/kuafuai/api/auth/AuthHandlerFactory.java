package com.kuafuai.api.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证处理器工厂
 */
@Component
@Slf4j
public class AuthHandlerFactory {

    @Autowired(required = false)
    private List<AuthHandler> authHandlers;

    private final Map<String, AuthHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (authHandlers != null) {
            for (AuthHandler handler : authHandlers) {
                String authType = handler.getAuthType();
                handlerMap.put(authType.toLowerCase(), handler);
                log.info("注册认证处理器: {}", authType);
            }
        }
        log.info("认证处理器工厂初始化完成, 共注册 {} 个处理器", handlerMap.size());
    }

    /**
     * 根据认证类型获取处理器
     *
     * @param authType 认证类型
     * @return 认证处理器, 如果不存在则返回null
     */
    public AuthHandler getHandler(String authType) {
        if (authType == null) {
            return null;
        }
        return handlerMap.get(authType.toLowerCase());
    }
}
