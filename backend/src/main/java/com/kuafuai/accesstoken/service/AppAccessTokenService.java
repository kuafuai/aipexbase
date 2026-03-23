package com.kuafuai.accesstoken.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.accesstoken.entity.AppAccessToken;

import java.util.List;

public interface AppAccessTokenService extends IService<AppAccessToken> {

    AppAccessToken getOrCreate(String appId);

    AppAccessToken reset(String appId);

    AppAccessToken getByToken(String token);

    AppAccessToken updateAllowedPaths(String appId, List<String> paths);
}
