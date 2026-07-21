package com.kuafuai.usage;

import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 拦截业务流量, 采集 (appId, endpointGroup, status, latency) 到 UsageAggregator.
 * <p>
 * 注册在 SecurityConfig 里 GlobalAppIdFilter 之后 (靠插入顺序), 保证 appId 已经落到 ThreadLocal.
 * 认证失败 (401) 的请求走不到这里, 不计入用量, 这符合预期 (不是真实业务流量).
 * <p>
 * 只收 /api/** 前缀; /admin/**、/mcp/**、静态资源等由 EndpointClassifier 判为 _ignore 后被聚合器忽略.
 * <p>
 * 注意: 故意不加 @Component. 由 SecurityConfig 手动 new 出来放到 filter chain, 避免被
 * Spring Boot 自动注册成全局 servlet filter (会拦截所有请求两次).
 * aggregator 通过 SpringUtils 懒加载, 不走构造注入.
 */
@Slf4j
public class UsageTrackingFilter extends OncePerRequestFilter {

    private volatile UsageAggregator aggregator;

    private UsageAggregator getAggregator() {
        UsageAggregator ref = aggregator;
        if (ref == null) {
            synchronized (this) {
                ref = aggregator;
                if (ref == null) {
                    ref = SpringUtils.getBean(UsageAggregator.class);
                    aggregator = ref;
                }
            }
        }
        return ref;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    @NotNull HttpServletResponse resp,
                                    @NotNull FilterChain chain) throws ServletException, IOException {
        String path = req.getRequestURI();
        log.info("==========={}============流量监控入口", path);
        // 快速过滤: 只对 /api/** 采集, 别的路径直接放行不进 hot path
        if (!path.startsWith("/api/")) {
            chain.doFilter(req, resp);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, resp);
        } finally {
            try {
                String appId = GlobalAppIdFilter.getAppId();
                if (StringUtils.isNotEmpty(appId)) {
                    String group = EndpointClassifier.classify(req.getMethod(), path);
                    getAggregator().record(appId, group, resp.getStatus(), System.currentTimeMillis() - start);
                }
            } catch (Exception e) {
                // 埋点异常绝不影响主流程
                log.warn("UsageTrackingFilter record failed: {}", e.getMessage());
            }
        }
    }
}
