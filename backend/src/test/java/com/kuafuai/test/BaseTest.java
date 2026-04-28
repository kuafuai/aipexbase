package com.kuafuai.test;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
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

    @Test
    public void test_date_change() {
        String value = "2026-04-12";
        String format = "yyyy-MM-dd HH:mm:ss";
        //DateTime dateTime = DateUtil.parse(value);
        //log.info("{}", dateTime);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withResolverStyle(ResolverStyle.STRICT);
        //formatter.parse(value);
        LocalDate.parse(value, formatter);
    }
}
