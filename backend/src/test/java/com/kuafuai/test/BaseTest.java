package com.kuafuai.test;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kuafuai.dynamic.validation.QueryConditionValidator;
import com.kuafuai.system.entity.AppTableColumnInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    @Test
    public void test_vaild(){
        List<AppTableColumnInfo> columns = Lists.newArrayList();
        columns.add(AppTableColumnInfo.builder()
                .dslType("date")
                .columnName("record_date")
                .build());

        Map<String,Object> rd = Maps.newHashMap();
        rd.put("gte", "2026-04-01");
        rd.put("lte", "2026-04-31");

        Map<String,Object> data = Maps.newHashMap();
        data.put("record_date", rd);

        QueryConditionValidator.validate(columns, data);
    }
}
