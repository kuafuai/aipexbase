package com.kuafuai.login.provider.oauth2.server;

import com.kuafuai.login.entity.OAuth2UserInfo;

/**
 * OAuth2提供商接口
 */
public interface OAuth2ProviderInterface {
    
    /**
     * 获取提供商名称
     */
    String getProviderName();
    
    /**
     * 根据授权码获取访问令牌
     * @param code 授权码
     * @return 访问令牌
     */
    String getAccessToken(String code);
    
    /**
     * 根据访问令牌获取用户信息
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    OAuth2UserInfo getUserInfo(String accessToken);
    
    /**
     * 生成授权URL
     * @param state 状态参数
     * @return 授权URL
     */
    String getAuthorizationUrl(String state);


    /**
     *
     * 获取重定向url
     */
    String getCallbackUri();
}
