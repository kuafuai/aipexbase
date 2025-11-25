package com.kuafuai.login.handle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.*;
import com.kuafuai.login.service.TokenService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;


@Slf4j
public class DynamicAuthFilter extends OncePerRequestFilter {

    private static final ThreadLocal<AppInfo> appInfoHolder = new ThreadLocal<>();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    //白名单直接放行
    private static final String[] WHITE_URLS = {
            "/login/**",
            "/oauth2/**",
            "/common/**",
            "/get_mp_url",
            "/generalOrder/callback/**",
            "/system/setting/**",
            "/error/report/**"
    };

    public static final String[] NON_VERIFIED_URLS = {
            "/profile/**",
            "/oauth2/callback/**",
            "/login/redirect/**",
            "/generalOrder/callback/**",
            "/error/report/**",
            "/mcp/**"

    };

    private final Type config_value_type = new TypeToken<Map<String, Object>>() {
    }.getType();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //不需要APP_ID，不需要验证，直接放行
        String requestUri = request.getRequestURI();
        if (isUrlMatched(NON_VERIFIED_URLS, requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String appId = GlobalAppIdFilter.getAppId();
        AppInfo appInfo = getAppInfoByAppId(appId);

        if (appInfo == null) {
            unauthorizedResponse(response, "error.code.not_found");
            return;
        }

        refreshLastActive(appInfo);

        // 需要APP_ID,不要验证，直接放行
        if (isUrlMatched(WHITE_URLS, requestUri)) {
            log.info("白名单路径: {}, 跳过认证", requestUri);
            proceedWithAppInfo(filterChain, request, response, appInfo);
            return;
        }

        // 读取APP config_json 查看是否是放行的表
        if (matchAppConfigReadTable(appInfo, request)) {
            proceedWithAppInfo(filterChain, request, response, appInfo);
            return;
        }

        String appType = GlobalAppIdFilter.getAppType();
        if (appInfo.getNeedAuth() || StringUtils.equalsIgnoreCase(appType, "admin")) {
            // 需要登录,验证登录信息是否存在
            LoginUser loginUser = getLoginUser(request);
            if (loginUser == null) {
                unauthorizedResponse(response, "error.code.not_login");
                return;
            }
        }

        proceedWithAppInfo(filterChain, request, response, appInfo);
    }

    private void proceedWithAppInfo(FilterChain chain, HttpServletRequest request, HttpServletResponse response, AppInfo appInfo)
            throws ServletException, IOException {
        try {
            appInfoHolder.set(appInfo);
            chain.doFilter(request, response);
        } finally {
            appInfoHolder.remove();
        }
    }

    private void unauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        int code = HttpStatus.UNAUTHORIZED;
        message = I18nUtils.getOrDefault(message, message);
        ServletUtils.renderString(response, JSON.toJSONString(ResultUtils.error(code, message)));
    }

    private boolean matchAppConfigReadTable(AppInfo appInfo, HttpServletRequest request) {
        String url = request.getRequestURI();
        if (StringUtils.equalsIgnoreCase(url, "/api/data/invoke")) {
            String table = request.getParameter("table");
            String method = request.getParameter("method");

            if (StringUtils.isNotEmpty(table) &&
                    StringUtils.isNotEmpty(method) &&
                    StringUtils.equalsAnyIgnoreCase(method, "get", "list", "page")) {

                Map<String, Object> configMap = JSON.parseObject(appInfo.getConfigJson(), config_value_type);

                List<String> readTables = Optional.ofNullable((List<String>) configMap.get("read_table"))
                        .orElse(Collections.emptyList());

                if (!readTables.isEmpty()) {
                    log.info("pass=================:read table pass {}:{}", appInfo.getAppId(), table);
                    return readTables.contains(table);
                }
            }
        }
        return false;
    }

    private boolean isUrlMatched(String[] patterns, String requestUri) {
        return Arrays.stream(patterns).anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

    private AppInfo getAppInfoByAppId(String appId) {
        AppInfoService appInfoService = SpringUtils.getBean(AppInfoService.class);

        LambdaQueryWrapper<AppInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppInfo::getAppId, appId);

        return appInfoService.getOne(queryWrapper);
    }

    private void refreshLastActive(AppInfo appInfo) {
        LocalDate today = LocalDate.now();
        LocalDate last = appInfo.getUpdatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (!last.isBefore(today)) {
            return;
        }

        AppInfoService appInfoService = SpringUtils.getBean(AppInfoService.class);
        appInfoService.refreshLastActive(appInfo.getAppId());
    }

    private LoginUser getLoginUser(HttpServletRequest request) {
        TokenService tokenService = SpringUtils.getBean(TokenService.class);
        return tokenService.getLoginUser(request);
    }

    public static AppInfo getAppInfo() {
        return appInfoHolder.get();
    }
}
