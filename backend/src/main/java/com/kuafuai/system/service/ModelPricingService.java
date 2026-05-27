package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.ModelPricing;

public interface ModelPricingService extends IService<ModelPricing> {

    ModelPricing getByModelName(String modelName);
}
