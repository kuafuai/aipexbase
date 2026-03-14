package com.kuafuai.test;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Locale;

@Slf4j
public class BaseTest {

    @Test
    public void test_date_formatter() {
        String value = "Sat Mar 14 2026 12:30:45 GMT+0800";
        DateTime dateTime;
        try {
            dateTime = DateUtil.parse(value);
        } catch (Exception e) {
            dateTime = DateUtil.parse(value, "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
        }
        log.info("{}", dateTime);

        String resultValue = DateUtil.format(dateTime, "yyyy-MM-dd");
        log.info("{}", resultValue);
    }
}
