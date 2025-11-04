package com.kuafuai.login.service.impl;

import com.kuafuai.common.login.LoginUser;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.entity.OAuth2UserInfo;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.login.provider.oauth2.server.OAuth2ProviderInterface;
import com.kuafuai.login.service.LoginBusinessService;
import com.kuafuai.login.service.OAuth2Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth2服务实现
 */
@Slf4j
@Service
public class OAuth2ServiceImpl implements OAuth2Service {
    
    @Resource
    private ApplicationContext applicationContext;
    
    @Resource
    private LoginBusinessService loginBusinessService;

    
    @Override
    public OAuth2ProviderInterface getProvider(String providerName) {
        Map<String, OAuth2ProviderInterface> providers = applicationContext.getBeansOfType(OAuth2ProviderInterface.class);
        for (OAuth2ProviderInterface provider : providers.values()) {
            if (provider.getProviderName().equalsIgnoreCase(providerName)) {
                return provider;
            }
        }
        return null;
    }
    
    @Override
    public LoginUser findOrCreateUser(OAuth2UserInfo userInfo, String relevanceTable) {
        String appId = GlobalAppIdFilter.getAppId();


        Login existingUser = null;
        if (StringUtils.hasText(userInfo.getId())) {
            existingUser = loginBusinessService.getUserByOpenId(userInfo.getId(), relevanceTable);
        }

        // 用户之前就存在过
        if (existingUser != null) {
            return loginBusinessService.getLoginUser(existingUser, relevanceTable);
        } else {
            // 创建新用户
            Login login = loginBusinessService.createNewLoginByOauth2(userInfo.getId(), relevanceTable, appId);
            return  loginBusinessService.getLoginUser(login,relevanceTable);
        }

    }
    
    @Override
    public String getAuthorizationUrl(String provider, String state) {
        OAuth2ProviderInterface oauth2Provider = getProvider(provider);
        if (oauth2Provider == null) {
            throw new RuntimeException("不支持的 OAuth2 提供商: " + provider);
        }
        
        if (!StringUtils.hasText(state)) {
            state = UUID.randomUUID().toString();
        }
        
        return oauth2Provider.getAuthorizationUrl(state);
    }

    @Override
    public String getCallbackUri(String provider) {
        OAuth2ProviderInterface oauth2Provider = getProvider(provider);
        if (oauth2Provider == null) {
            throw new RuntimeException("不支持的 OAuth2 提供商: " + provider);
        }

        return oauth2Provider.getCallbackUri();
    }

    /**
     * 更新用户的OAuth2信息
//     */
//    private void updateUserOAuth2Info(Login user, OAuth2UserInfo userInfo) {
//        boolean needUpdate = false;
//
//        // 更新头像
//        if (StringUtils.hasText(userInfo.getAvatar()) && !userInfo.getAvatar().equals(user.getWxOpenId())) {
//            user.setWxOpenId(userInfo.getAvatar()); // 这里复用wxOpenId字段存储头像URL
//            needUpdate = true;
//        }
//
//        // 更新用户名
//        if (StringUtils.hasText(userInfo.getUsername()) && !userInfo.getUsername().equals(user.getUserName())) {
//            user.setUserName(userInfo.getUsername());
//            needUpdate = true;
//        }
//
//        // 更新手机号（如果OAuth2提供了手机号）
//        if (StringUtils.hasText(userInfo.getPhone()) && !userInfo.getPhone().equals(user.getPhoneNumber())) {
//            user.setPhoneNumber(userInfo.getPhone());
//            needUpdate = true;
//        }
//
//        if (needUpdate) {
//            try {
//                // 更新Login记录
//                loginService.updateById(user);
//
//                // 更新用户表中的信息
//                Map<String, Object> updateData = new HashMap<>();
//                if (StringUtils.hasText(userInfo.getUsername())) {
//                    updateData.put("username", userInfo.getUsername());
//                }
//                if (StringUtils.hasText(userInfo.getEmail())) {
//                    updateData.put("email", userInfo.getEmail());
//                }
//                if (StringUtils.hasText(userInfo.getAvatar())) {
//                    updateData.put("avatar", userInfo.getAvatar());
//                }
//                if (StringUtils.hasText(userInfo.getNickname())) {
//                    updateData.put("nickname", userInfo.getNickname());
//                }
//
//                if (!updateData.isEmpty()) {
//                    String appId = GlobalAppIdFilter.getAppId();
//                    dynamicInterfaceService.updateById(appId, user.getRelevanceTable(), Long.valueOf(user.getRelevanceId()), updateData);
//                }
//
//                log.info("成功更新用户OAuth2信息: userId={}", user.getLoginId());
//            } catch (Exception e) {
//                log.error("更新用户OAuth2信息失败: userId={}, error={}", user.getLoginId(), e.getMessage());
//            }
//        }
//    }

}
