package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.ManageApiToken;

/**
 * 管理API Token服务接口
 */
public interface ManageApiTokenService extends IService<ManageApiToken> {

    /**
     * 根据Token字符串查询
     */
    ManageApiToken getByToken(String token);

    /**
     * 更新最后使用时间
     */
    void updateLastUsedTime(Long id);
}
