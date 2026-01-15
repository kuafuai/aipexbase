package com.kuafuai.login.handle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.util.*;
import com.kuafuai.config.db.DatabaseRouterAspect;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import com.kuafuai.system.entity.APIKey;
import com.kuafuai.system.service.ApplicationAPIKeysService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
public class GlobalAppIdFilter extends OncePerRequestFilter {
    private static final ThreadLocal<String> appIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> appTypeHolder = new ThreadLocal<>();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public static final String[] NON_VERIFIED_URLS = {
            "/profile/**",
            "/login/redirect/**",
            "/generalOrder/callback/**",
            "/error/report/**",
            "/mcp/**",
            "/oauth2/callback/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    @NotNull HttpServletResponse httpServletResponse,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        //不需要APP_ID，不需要验证，直接放行
        String requestUri = httpServletRequest.getRequestURI();
        if (isExcludedUrl(requestUri)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        String appId = getHeaderIgnoreCase(httpServletRequest, "APP_ID");
        String appType = getHeaderIgnoreCase(httpServletRequest, "APP_TYPE", "user");
        String apiKey = getHeaderIgnoreCase(httpServletRequest, "CODE_FLYING");

        String rdsKey;
        if (StringUtils.isNotEmpty(appId)) {
            rdsKey = DatabaseRouterAspect.getOrAllocateRdsKey(appId, "app");
        } else if (StringUtils.isNotEmpty(apiKey)) {
            rdsKey = DatabaseRouterAspect.getOrAllocateRdsKey(apiKey, "token");
        } else {
            writeUnauthorizedResponse(httpServletResponse, "error.code.no_auth");
            return;
        }

        try {
            
            DynamicDataSourceContextHolder.setDataSourceType(rdsKey);

            if (StringUtils.isEmpty(appId) && StringUtils.isNotEmpty(apiKey)) {
                APIKey key = getAPIKey(apiKey);
                if (Objects.isNull(key)) {
                    writeUnauthorizedResponse(httpServletResponse, "error.code.no_auth");
                    return;
                }
                appId = key.getAppId();
            }

            if (StringUtils.isEmpty(appId)) {
                writeUnauthorizedResponse(httpServletResponse, "error.code.params_error");
                return;
            }

            appIdHolder.set(appId);
            appTypeHolder.set(appType);

            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            log.info("Clear ThreadLocal: APP_ID={}, APP_TYPE={}", appId, appType);
            appIdHolder.remove();
            appTypeHolder.remove();

            DynamicDataSourceContextHolder.clearDataSourceType();
        }
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

    private void writeUnauthorizedResponse(HttpServletResponse response, String msg) throws IOException {
        int code = HttpStatus.UNAUTHORIZED;
        msg = I18nUtils.getOrDefault(msg, msg);
        String body = JSON.toJSONString(ResultUtils.error(code, msg));
        ServletUtils.renderString(response, body);
    }

    private boolean isExcludedUrl(String requestUri) {
        return Arrays.stream(NON_VERIFIED_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

    public static String getAppId() {
        return appIdHolder.get();
    }


    public static String getAppType() {
        return appTypeHolder.get();
    }

    private APIKey getAPIKey(String key) {

        ApplicationAPIKeysService applicationAPIKeysService = SpringUtils.getBean(ApplicationAPIKeysService.class);
        LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(APIKey::getKeyName, key);
        queryWrapper.eq(APIKey::getStatus, APIKey.APIKeyStatus.ACTIVE.name());
        queryWrapper.gt(APIKey::getExpireAt, DateUtils.getTime());

        return applicationAPIKeysService.getOne(queryWrapper);
    }

    public static void setAppId(String appId) {
        appIdHolder.set(appId);
    }
}
