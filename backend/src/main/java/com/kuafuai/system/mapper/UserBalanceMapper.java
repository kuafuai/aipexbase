package com.kuafuai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.system.entity.UserBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserBalanceMapper extends BaseMapper<UserBalance> {

    /**
     * 扣减用户余额
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return 影响行数
     */
    @Update("UPDATE user_balance SET balance = balance - #{amount}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND balance >= #{amount} AND status = 1")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    
    /**
     * 增加用户余额
     * @param userId 用户ID
     * @param amount 增加金额
     * @return 影响行数
     */
    @Update("UPDATE user_balance SET balance = balance + #{amount}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND status = 1")
    int increaseBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}