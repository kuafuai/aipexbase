package com.kuafuai.common.login;


import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
public class LoginUser implements UserDetails {

    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;


    private String relevanceId;


    private String relevanceTable;

    private Integer tenantId;

    private String appId;

    /**
     * 是否绕过 RLS 检查
     * true: 跳过 RLS，可以访问所有数据（管理员模式）
     * false/null: 正常应用 RLS 策略
     */
    private Boolean bypassRls;


    public LoginUser(String appId, String relevanceId, String relevanceTable) {
        this.appId = appId;
        this.relevanceId = relevanceId;
        this.relevanceTable = relevanceTable;
    }

    public LoginUser(String appId, String relevanceId, String relevanceTable, Boolean passRls) {
        this.appId = appId;
        this.relevanceId = relevanceId;
        this.relevanceTable = relevanceTable;
        this.bypassRls = passRls;
    }

    public LoginUser(Long userId) {
        this.userId = userId;
        this.bypassRls = true;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return null;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }


    @Override
    public boolean isAccountNonLocked() {
        return true;
    }


    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    @Override
    public boolean isEnabled() {
        return true;
    }
}
