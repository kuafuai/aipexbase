package com.kuafuai.controller;

import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.mail.client.MailClient;
import com.kuafuai.common.mail.spec.MailDefinition;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
public class MessageController {
    private final MailClient client = new MailClient();

    @Autowired
    private DynamicConfigBusinessService dynamicConfigBusinessService;

    private static final String CONFIG_MAIL_HOST = "mail.host";
    private static final String CONFIG_MAIL_USER = "mail.user";
    private static final String CONFIG_MAIL_PASSWD = "mail.passwd";
    private static final String CONFIG_MAIL_PORT = "mail.port";

    @PostMapping("/common/mail/send")
    public BaseResponse mailSend(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("title")) {
            return ResultUtils.error("title参数不存在，请检查参数！");
        }
        if (!data.containsKey("content")) {
            return ResultUtils.error("content参数不存在，请检查参数！");
        }
        if (!data.containsKey("mail")) {
            return ResultUtils.error("mail参数不存在，请检查参数！");
        }

        String title = Objects.toString(data.get("title"), "");
        String content = Objects.toString(data.get("content"), "");
        String mail = Objects.toString(data.get("mail"), "");
        Map<String, String> params = Maps.newHashMap();
        if (data.containsKey("params") && data.get("params") instanceof Map) {
            Map<?, ?> paramMap = (Map<?, ?>) data.get("params");
            for (Map.Entry<?, ?> entry : paramMap.entrySet()) {
                params.put(
                        Objects.toString(entry.getKey(), ""),
                        Objects.toString(entry.getValue(), "")
                );
            }
        }

        MailDefinition definition = createMailDefinition(GlobalAppIdFilter.getAppId(), content);

        client.send(definition, mail, title, params);
        return ResultUtils.success("邮件发送成功");
    }

    private MailDefinition createMailDefinition(String appId, String content) {
        Map<String, String> configMap = dynamicConfigBusinessService.getSystemConfig(appId);
        String host = configMap.getOrDefault(CONFIG_MAIL_HOST, "smtp.126.com");
        String userName = configMap.getOrDefault(CONFIG_MAIL_USER, "");
        String password = configMap.getOrDefault(CONFIG_MAIL_PASSWD, "");
        Integer port = Integer.parseInt(configMap.getOrDefault(CONFIG_MAIL_PORT, "465"));

        return MailDefinition.builder()
                .host(host)
                .port(port)
                .userName(userName)
                .password(password)
                .contentTemplate(content)
                .build();
    }
}
