package com.kuafuai.manage.filter;

import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.util.*;
import com.kuafuai.manage.config.ManageApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 管理API认证过滤器
 * 用于验证对外开放的管理接口的访问权限
 */
@Slf4j
public class ManageApiAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        ManageApiConfig manageApiConfig = SpringUtils.getBean(ManageApiConfig.class);

        // 检查开关是否启用
        if (!manageApiConfig.isEnable()) {
            log.warn("管理API未启用，请求路径: {}", request.getRequestURI());
            writeUnauthorizedResponse(response, "error.manage.api.disabled");
            return;
        }

        // 获取请求头中的token
        String token = request.getHeader("X-Manage-Token");
        if (StringUtils.isEmpty(token)) {
            token = request.getParameter("token");
        }

        // 验证token
        if (StringUtils.isEmpty(token)) {
            log.warn("管理API Token为空，请求路径: {}", request.getRequestURI());
            writeUnauthorizedResponse(response, "error.manage.api.token.missing");
            return;
        }

        if (!token.equals(manageApiConfig.getToken())) {
            log.warn("管理API Token验证失败，请求路径: {}", request.getRequestURI());
            writeUnauthorizedResponse(response, "error.manage.api.token.invalid");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 写入未授权响应
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String msg) throws IOException {
        int code = HttpStatus.UNAUTHORIZED;
        msg = I18nUtils.getOrDefault(msg, msg);
        String body = JSON.toJSONString(ResultUtils.error(code, msg));
        ServletUtils.renderString(response, body);
    }
}
