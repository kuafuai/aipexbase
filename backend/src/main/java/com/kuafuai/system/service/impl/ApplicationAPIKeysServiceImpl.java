package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.common.util.DateUtils;
import com.kuafuai.system.entity.APIKey;
import com.kuafuai.system.mapper.APIKeyMapper;
import com.kuafuai.system.service.ApplicationAPIKeysService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAPIKeysServiceImpl extends ServiceImpl<APIKeyMapper, APIKey> implements ApplicationAPIKeysService {
    @Override
    public APIKey getApiKey(String apiKey) {
        LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(APIKey::getKeyName, apiKey);
        queryWrapper.eq(APIKey::getStatus, APIKey.APIKeyStatus.ACTIVE.name());
        queryWrapper.gt(APIKey::getExpireAt, DateUtils.getTime());

        return getOne(queryWrapper);
    }

    @Override
    public boolean deleteByAppId(String appId) {
        LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(APIKey::getAppId, appId);
        return remove(queryWrapper);
    }
}
