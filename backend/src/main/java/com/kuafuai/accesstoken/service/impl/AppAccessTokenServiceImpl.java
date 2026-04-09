package com.kuafuai.accesstoken.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.kuafuai.accesstoken.entity.AppAccessToken;
import com.kuafuai.accesstoken.mapper.AppAccessTokenMapper;
import com.kuafuai.accesstoken.service.AppAccessTokenService;
import com.kuafuai.common.db.DynamicDataBaseServiceImpl;
import com.kuafuai.common.util.IdUtils;
import com.kuafuai.common.util.JSON;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class AppAccessTokenServiceImpl extends DynamicDataBaseServiceImpl<AppAccessTokenMapper, AppAccessToken> implements AppAccessTokenService {

    private static final List<String> DEFAULT_ALLOWED_PATHS = Lists.newArrayList("/api/data/invoke","/api/word2pic");

    @Override
    public AppAccessToken getOrCreate(String appId) {
        AppAccessToken existing = getByAppId(appId);
        if (existing != null) {
            return existing;
        }
        return create(appId);
    }

    @Override
    @Transactional
    public AppAccessToken reset(String appId) {
        AppAccessToken existing = getByAppId(appId);
        String allowedPaths = existing != null ? existing.getAllowedPaths() : null;

        LambdaQueryWrapper<AppAccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppAccessToken::getAppId, appId);
        remove(wrapper);

        AppAccessToken fresh = create(appId);
        fresh.setAllowedPaths(allowedPaths);
        updateById(fresh);
        return fresh;
    }

    @Override
    public AppAccessToken getByToken(String token) {
        LambdaQueryWrapper<AppAccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppAccessToken::getToken, token);
        return getOne(wrapper);
    }

    @Override
    public AppAccessToken updateAllowedPaths(String appId, List<String> paths) {
        AppAccessToken accessToken = getOrCreate(appId);
        accessToken.setAllowedPaths(paths == null || paths.isEmpty() ? null : JSON.toJSONString(paths));
        accessToken.setUpdatedAt(new Date());
        updateById(accessToken);
        return accessToken;
    }

    private AppAccessToken getByAppId(String appId) {
        LambdaQueryWrapper<AppAccessToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppAccessToken::getAppId, appId);
        return getOne(wrapper);
    }

    private AppAccessToken create(String appId) {
        AppAccessToken accessToken = AppAccessToken.builder()
                .appId(appId)
                .token("kft_" + IdUtils.fastSimpleUUID())
                .allowedPaths(JSON.toJSONString(DEFAULT_ALLOWED_PATHS))
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        save(accessToken);
        return accessToken;
    }
}
