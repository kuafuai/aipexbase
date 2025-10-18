package com.kuafuai.login.service;

import cn.hutool.core.util.RandomUtil;
import com.google.common.collect.Maps;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.mail.client.MailClient;
import com.kuafuai.common.mail.spec.MailDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class LoginMailBusinessService {

    private final MailClient client = new MailClient();
    @Resource
    private Cache cache;

    @Autowired
    private DynamicConfigBusinessService dynamicConfigBusinessService;

    private static final String CACHE_PREFIX = "login:mail:";
    private static final String CONFIG_MAIL_HOST = "mail.host";
    private static final String CONFIG_MAIL_USER = "mail.user";
    private static final String CONFIG_MAIL_PASSWD = "mail.passwd";
    private static final String CONFIG_MAIL_PORT = "mail.port";
    private static final String CONFIG_MAIL_TEMPLATE = "login.mail.code_template";
    private static final String CONFIG_MAIL_SUBJECT = "login.mail.code_subject";
    private static final int LOGIN_CODE_EXPIRE_MINUTES = 5;


    public String sendLoginCode(String appId, String mail) {
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);

        String numbers = RandomUtil.randomNumbers(6);

        //配置了host等参数，走发送流程
        if (map.containsKey(CONFIG_MAIL_HOST)) {
            String subject = resolveCodeSubject(map);
            MailDefinition definition = createMailDefinition(map);

            Map<String, String> codeMap = Maps.newHashMap();
            codeMap.put("code", numbers);
            client.send(definition, mail, subject, codeMap);

            cache.setCacheObject(buildCacheKey(mail), numbers, LOGIN_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return "";
        } else {
            //没有配置走mock流程
            cache.setCacheObject(buildCacheKey(mail), numbers, LOGIN_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return numbers;
        }
    }

    public String getLoginCode(String mail) {
        return cache.getCacheObject(buildCacheKey(mail));
    }

    private MailDefinition createMailDefinition(Map<String, String> configMap) {
        String host = configMap.getOrDefault(CONFIG_MAIL_HOST, "smtp.126.com");
        String userName = configMap.getOrDefault(CONFIG_MAIL_USER, "kuafuai@126.com");
        String password = configMap.getOrDefault(CONFIG_MAIL_PASSWD, "HOXVPZDIOXTNYJAX");
        Integer port = Integer.parseInt(configMap.getOrDefault(CONFIG_MAIL_PORT, "465"));

        String mailTemplate = configMap.getOrDefault(CONFIG_MAIL_TEMPLATE, "您的验证码: ${{code}}");

        return MailDefinition.builder()
                .host(host)
                .port(port)
                .userName(userName)
                .password(password)
                .contentTemplate(mailTemplate)
                .build();
    }

    private String resolveCodeSubject(Map<String, String> configMap) {
        return configMap.getOrDefault(CONFIG_MAIL_SUBJECT, "邮箱验证码");
    }

    private String buildCacheKey(String mail) {
        return CACHE_PREFIX + mail;
    }
}
