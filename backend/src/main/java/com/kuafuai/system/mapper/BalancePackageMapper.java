package com.kuafuai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.system.entity.BalancePackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface BalancePackageMapper extends BaseMapper<BalancePackage> {

    /**
     * 原子扣减指定包的余额（乐观锁：remaining >= deductAmount）
     */
    @Update("UPDATE balance_package SET remaining_amount = remaining_amount - #{amount}, " +
            "status = IF(remaining_amount - #{amount} <= 0, 0, status), " +
            "updated_at = NOW() " +
            "WHERE id = #{id} AND remaining_amount >= #{amount} AND status = 1")
    int deductPackage(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 批量将过期包标记为无效
     */
    @Update("UPDATE balance_package SET status = 0, updated_at = NOW() " +
            "WHERE codeflying_user_id = #{userId} AND status = 1 AND expired_at <= NOW()")
    int expirePackages(@Param("userId") String userId);
}
