package com.kuafuai.config;

import org.springframework.core.convert.converter.Converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StringToDateConverter implements Converter<String, Date> {

    private static final List<SimpleDateFormat> dateFormats = new ArrayList<>();

    static {
        dateFormats.add(new SimpleDateFormat("yyyy-MM-dd"));
        dateFormats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        dateFormats.add(new SimpleDateFormat("yyyy/MM/dd"));
        dateFormats.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
        // 你可以根据需要添加更多格式
    }

    @Override
    public Date convert(String source) {
        for (SimpleDateFormat format : dateFormats) {
            try {
                return format.parse(source);
            } catch (ParseException e) {
                // 忽略并尝试下一个格式
            }
        }
        throw new RuntimeException("无法解析日期: " + source);
    }
}
