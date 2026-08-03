package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import com.kuafuai.system.entity.PointsRecord;
import com.kuafuai.system.mapper.PointsRecordMapper;
import com.kuafuai.system.service.PointsRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements PointsRecordService {

    @Override
    public List<PointsRecord> getRecordsByUser(String codeFlyingUserId, int limit) {
        LambdaQueryWrapper<PointsRecord> query = new LambdaQueryWrapper<>();
        query.eq(PointsRecord::getCodeFlyingUserId, codeFlyingUserId)
             .orderByDesc(PointsRecord::getCreatedAt)
             .last("LIMIT " + limit);
        return list(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void saveToDefault(String codeFlyingUserId, String type, String subType, BigDecimal amount) {
        try {
            PointsRecord record = PointsRecord.builder()
                    .codeFlyingUserId(codeFlyingUserId)
                    .type(type)
                    .subType(subType)
                    .amount(amount)
                    .createdAt(new Date())
                    .build();
            save(record);
        } catch (Exception e) {
            log.warn("保存积分记录失败, codeFlyingUserId: {}, type: {}, subType: {}, amount: {}, error: {}",
                    codeFlyingUserId, type, subType, amount, e.getMessage());
        }
    }
}
