package com.kuafuai.common.dynamic_config.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DynamicConfigBusinessServiceTest {

    @Resource
    private DynamicConfigBusinessService dynamicConfigBusinessService;
    @Test
    void getSystemConfig() {
        System.out.println(dynamicConfigBusinessService.getSystemConfig("1700330478"));
        System.out.println(dynamicConfigBusinessService.getSystemConfig("1700330478"));
        System.out.println(dynamicConfigBusinessService.getSystemConfig("1700330478"));
        System.out.println(dynamicConfigBusinessService.getSystemConfig("1700330478"));
        System.out.println(dynamicConfigBusinessService.getSystemConfig("1700330478"));
        System.out.println(dynamicConfigBusinessService.getSystemConfig("1700330478"));
        System.out.println(dynamicConfigBusinessService.getSystemConfig("1700330478"));
    }
}