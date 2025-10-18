package com.kuafuai.login.handle;


import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.service.TokenService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;


@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    public static final String[] NON_VERIFIED_URLS = {
            "/login/redirect/**",
            "/generalOrder/callback/**",
            "/error/report/**",
            "/mcp/**"
    };


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        if (isExcludedUrl(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        TokenService tokenService = SpringUtils.getBean(TokenService.class);
        LoginUser loginUser = tokenService.getLoginUser(request);

        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication())) {
            boolean flag = tokenService.verifyToken(loginUser, GlobalAppIdFilter.getAppId());
            if (flag) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else {
                // 登录失效
                int code = HttpStatus.UNAUTHORIZED;
                String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());
                ServletUtils.renderString(response, JSON.toJSONString(ResultUtils.error(code, msg)));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isExcludedUrl(String requestUri) {
        return Arrays.stream(NON_VERIFIED_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }
}
