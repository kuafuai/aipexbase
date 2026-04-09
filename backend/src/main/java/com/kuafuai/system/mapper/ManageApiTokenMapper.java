package com.kuafuai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.system.entity.ManageApiToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 管理API Token Mapper
 */
@Mapper
public interface ManageApiTokenMapper extends BaseMapper<ManageApiToken> {

    /**
     * 更新最后使用时间
     */
    @Update("UPDATE manage_api_token SET last_used_time = NOW() WHERE id = #{id}")
    int updateLastUsedTime(@Param("id") Long id);
}
