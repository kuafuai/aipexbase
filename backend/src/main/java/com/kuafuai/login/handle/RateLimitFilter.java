package com.kuafuai.login.handle;

import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.service.LocalAppRateLimitService;
import com.kuafuai.common.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取 appId 和 apiKey
        String appId = getHeaderIgnoreCase(httpServletRequest, "APP_ID");
        String apiKey = getHeaderIgnoreCase(httpServletRequest, "CODE_FLYING");
        String clientIp = ServletUtils.getClientIp();

        // 如果既没有 appId 也没有 apiKey，直接放行（后续 Filter 会处理）
        if (StringUtils.isEmpty(appId) && StringUtils.isEmpty(apiKey)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        // 使用 appId 或 apiKey 作为限流键
        String rateLimitKey = StringUtils.isNotEmpty(appId) ? appId : apiKey;
        LocalAppRateLimitService rateLimitService = SpringUtils.getBean(LocalAppRateLimitService.class);
        boolean allowed = rateLimitService.isAllowed(rateLimitKey, clientIp);
        if (!allowed) {
            log.info("================触发限流============{}:{}", rateLimitKey, clientIp);
            writeRateLimitResponse(httpServletResponse);
            return;
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private String getHeaderIgnoreCase(HttpServletRequest request, String key) {
        return getHeaderIgnoreCase(request, key, null);
    }

    private String getHeaderIgnoreCase(HttpServletRequest request, String key, String defaultValue) {
        String value = request.getHeader(key);
        if (StringUtils.isEmpty(value)) {
            value = request.getHeader(key.toLowerCase());
        }
        return StringUtils.defaultIfEmpty(value, defaultValue);
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        int code = HttpStatus.TOO_MANY_REQUESTS;
        String message = I18nUtils.getOrDefault("error.rate_limit.too_many_requests", "Too many requests");
        String body = JSON.toJSONString(ResultUtils.error(code, message));
        ServletUtils.renderString(response, body);
    }
}
