package com.kuafuai.system.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.APIKey;

public interface ApplicationAPIKeysService extends IService<APIKey> {
    APIKey getApiKey(String apiKey);
}
