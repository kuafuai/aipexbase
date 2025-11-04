package com.kuafuai.login.service;

import com.kuafuai.common.login.LoginUser;
import com.kuafuai.login.entity.OAuth2UserInfo;
import com.kuafuai.login.provider.oauth2.server.OAuth2ProviderInterface;

/**
 * OAuth2服务接口
 */
public interface OAuth2Service {
    
    /**
     * 获取OAuth2提供商
     * @param providerName 提供商名称
     * @return 提供商实例
     */
    OAuth2ProviderInterface getProvider(String providerName);
    
    /**
     * 查找或创建用户
     * @param userInfo OAuth2用户信息
     * @param relevanceTable 关联表
     * @return 登录用户信息
     */
    LoginUser findOrCreateUser(OAuth2UserInfo userInfo, String relevanceTable);
    
    /**
     * 生成授权URL
     * @param provider 提供商名称
     * @param state 状态参数
     * @return 授权URL
     */
    String getAuthorizationUrl(String provider, String state);


    /**
     * 生成授权URL
     * @param provider 提供商名称
     * @return 授权URL
     */
    String getCallbackUri(String provider);
}
