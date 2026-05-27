package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.ModelPricing;
import com.kuafuai.system.mapper.ModelPricingMapper;
import com.kuafuai.system.service.ModelPricingService;
import org.springframework.stereotype.Service;

@Service
public class ModelPricingServiceImpl extends ServiceImpl<ModelPricingMapper, ModelPricing> implements ModelPricingService {

    @Override
    public ModelPricing getByModelName(String modelName) {
        LambdaQueryWrapper<ModelPricing> qw = new LambdaQueryWrapper<>();
        qw.eq(ModelPricing::getModelName, modelName)
          .eq(ModelPricing::getStatus, 1);
        return getOne(qw);
    }
}
