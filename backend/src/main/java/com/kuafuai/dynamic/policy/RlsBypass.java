package com.kuafuai.dynamic.policy;

import com.kuafuai.common.login.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/**
 * 以 RLS 旁路身份执行一段逻辑。
 *
 * 用于 register 等"用户身份尚未建立"的写入路径：临时把 SecurityContext.Authentication
 * 替换为带 bypassRls=true 的 LoginUser（与管理后台同款机制），执行 action，
 * finally 恢复原 Authentication。
 */
public final class RlsBypass {

    private RlsBypass() {
    }

    public static <T> T runAs(String appId, Supplier<T> action) {
        SecurityContext ctx = SecurityContextHolder.getContext();
        Authentication original = ctx.getAuthentication();
        try {
            LoginUser anon = new LoginUser(appId, null, null, true);
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(anon, null, anon.getAuthorities());
            ctx.setAuthentication(token);
            return action.get();
        } finally {
            ctx.setAuthentication(original);
        }
    }
}
