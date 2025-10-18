package com.kuafuai.test;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpUtil;
import com.google.common.collect.Maps;
import com.kuafuai.common.mail.client.MailClient;
import com.kuafuai.common.mail.spec.MailDefinition;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Slf4j
public class BaseTest {

    @Test
    public void test_contains() {
        String value = "asdfadsf\"adfasdfasdf";
        if (StringUtils.contains(value, "\"")) {
            log.info("111");
        }
    }

    @Test
    public void test_message() {
        final HashMap<String, Object> body = new HashMap<>();
        body.put("msg_type", "text");
        final HashMap<String, Object> contentMap = new HashMap<>();
        contentMap.put("text", "test");

        body.put("content", contentMap);
        HttpUtil.post("https://open.feishu.cn/open-apis/bot/v2/hook/e11be04d-b997-4c7a-b156-c442022d2d87", JSON.toJSONString(body));
    }

    @Test
    public void test_time() {
        log.info("{}", test_isValidTimeFormat("24:00:00", "HH:mm:ss"));
        log.info("{}", test_isValidTimeFormat("2023-13-41", "yyyy-MM-dd"));
        log.info("{}", test_isValidTimeFormat("2023-12-31 24:30:45", "yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    public void test_number() {
        String value = "-10";
        log.info("{}", NumberUtil.isNumber(value));
        log.info("{}", NumberUtil.isInteger(value));
        log.info("{}", NumberUtil.isNumber("0.1"));
    }

    @Test
    public void test_email() throws MessagingException {

        Map<String, String> params = Maps.newHashMap();
        params.put("code", "123");

        MailDefinition definition = MailDefinition.builder()
                .host("smtp.126.com")
                .port(465)
                .userName("kuafuai@126.com")
                .password("HOXVPZDIOXTNYJAX")
                .contentTemplate("您的验证码：${{code}}")
                .build();

        MailClient client = new MailClient();
        client.send(definition, "jiangfei@kuafuai.net", "邮箱验证码", params);

    }

    public boolean test_isValidTimeFormat(String timeStr, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withResolverStyle(ResolverStyle.STRICT);
            formatter.parse(timeStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
