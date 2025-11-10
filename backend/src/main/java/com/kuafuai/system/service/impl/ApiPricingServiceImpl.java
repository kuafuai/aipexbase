package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.ApiPricing;
import com.kuafuai.system.mapper.ApiPricingMapper;
import com.kuafuai.system.service.ApiPricingService;
import org.springframework.stereotype.Service;

@Service
public class ApiPricingServiceImpl extends ServiceImpl<ApiPricingMapper, ApiPricing> implements ApiPricingService {
}
