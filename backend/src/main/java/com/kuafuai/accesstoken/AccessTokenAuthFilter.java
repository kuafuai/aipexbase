package com.kuafuai.accesstoken;

import com.kuafuai.accesstoken.entity.AppAccessToken;
import com.kuafuai.accesstoken.service.AppAccessTokenService;
import com.kuafuai.accesstoken.strategy.AccessPathStrategy;
import com.kuafuai.accesstoken.strategy.AccessPathStrategyRegistry;
import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class AccessTokenAuthFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "kft_";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNotEmpty(token) && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }
        if (StringUtils.isEmpty(token) || !token.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        AppAccessTokenService service = SpringUtils.getBean(AppAccessTokenService.class);
        AppAccessToken accessToken = service.getByToken(token);

        if (accessToken == null) {
            log.warn("ACCESS_TOKEN invalid: {}", token);
            ServletUtils.renderString(response,
                    JSON.toJSONString(ResultUtils.error(HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN invalid")));
            return;
        }

        if (!isPathAllowed(accessToken, request)) {
            log.warn("ACCESS_TOKEN rejected: appId={}, path={}", accessToken.getAppId(), request.getRequestURI());
            ServletUtils.renderString(response,
                    JSON.toJSONString(ResultUtils.error(ErrorCode.NO_AUTH_ERROR.getCode(), ErrorCode.NO_AUTH_ERROR.getMessage())));
            return;
        }

        String currentAppId = GlobalAppIdFilter.getAppId();
        if (!accessToken.getAppId().equals(currentAppId)) {
            log.warn("ACCESS_TOKEN appId mismatch: token={}, header={}", accessToken.getAppId(), currentAppId);
            ServletUtils.renderString(response,
                    JSON.toJSONString(ResultUtils.error(HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN appId mismatch")));
            return;
        }

        LoginUser loginUser = new LoginUser(accessToken.getAppId(), null, null);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.debug("ACCESS_TOKEN auth passed: appId={}, path={}", accessToken.getAppId(), request.getRequestURI());
        chain.doFilter(request, response);
    }

    /**
     * 1. allowedPaths 为空 → 放行所有路径
     * 2. 请求路径不在 allowedPaths → 拒绝
     * 3. 路径命中 → 查找对应策略，有策略则执行二次校验，无策略直接放行
     */
    private boolean isPathAllowed(AppAccessToken token, HttpServletRequest request) {
        String allowedPathsJson = token.getAllowedPaths();
        if (StringUtils.isEmpty(allowedPathsJson)) {
            return true;
        }

        List<String> paths = JSON.parseArray(allowedPathsJson, String.class);
        if (paths == null || paths.isEmpty()) {
            return true;
        }

        String requestUri = request.getRequestURI();
        boolean pathMatched = paths.stream().anyMatch(p -> pathMatcher.match(p, requestUri));
        if (!pathMatched) {
            return false;
        }

        AccessPathStrategyRegistry registry = SpringUtils.getBean(AccessPathStrategyRegistry.class);
        Optional<AccessPathStrategy> strategy = registry.find(requestUri);
        return strategy.map(s -> s.isAllowed(token, request)).orElse(true);
    }
}

