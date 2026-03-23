package com.kuafuai.accesstoken.strategy;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;

/**
 * 策略注册中心，自动收集所有 AccessPathStrategy Bean。
 * Filter 通过此类按请求路径查找匹配的策略。
 */
@Component
public class AccessPathStrategyRegistry {

    private final List<AccessPathStrategy> strategies;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AccessPathStrategyRegistry(List<AccessPathStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 查找第一个 pathPattern 匹配请求路径的策略。
     */
    public Optional<AccessPathStrategy> find(String requestUri) {
        return strategies.stream()
                .filter(s -> pathMatcher.match(s.pathPattern(), requestUri))
                .findFirst();
    }
}
