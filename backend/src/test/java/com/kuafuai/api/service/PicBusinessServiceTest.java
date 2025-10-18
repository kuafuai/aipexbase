package com.kuafuai.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class PicBusinessServiceTest {

    @Resource
    private PicBusinessService picBusinessService;
    @Test
    void text2pic() {
//        System.out.println(picBusinessService.text2pic("4287", "生成一个大熊猫的图片"));
    }
}