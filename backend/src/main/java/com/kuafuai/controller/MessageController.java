package com.kuafuai.controller;

import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.mail.client.MailClient;
import com.kuafuai.common.mail.spec.MailDefinition;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${mail.default.host:smtp.126.com}")
    private String host;

    @Value("${mail.default.user:user}")
    private String user;

    @Value("${mail.default.passwd:123456}")
    private String passwd;

    @Value("${mail.default.port:465}")
    private String port;

    @PostMapping("/common/mail/send")
    public BaseResponse mailSend(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("title")) {
            return ResultUtils.error("login.register.params", "title");
        }
        if (!data.containsKey("content")) {
            return ResultUtils.error("login.register.params", "content");
        }
        if (!data.containsKey("to")) {
            return ResultUtils.error("login.register.params", "to");
        }

        String title = Objects.toString(data.get("title"), "");
        String content = Objects.toString(data.get("content"), "");
        String mail = Objects.toString(data.get("to"), "");
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
        return ResultUtils.success();
    }

    private MailDefinition createMailDefinition(String appId, String content) {
        Map<String, String> configMap = dynamicConfigBusinessService.getSystemConfig(appId);
        String host = configMap.getOrDefault(CONFIG_MAIL_HOST, this.host);
        String userName = configMap.getOrDefault(CONFIG_MAIL_USER, this.user);
        String password = configMap.getOrDefault(CONFIG_MAIL_PASSWD, this.passwd);
        Integer port = Integer.parseInt(configMap.getOrDefault(CONFIG_MAIL_PORT, this.port));

        return MailDefinition.builder()
                .host(host)
                .port(port)
                .userName(userName)
                .password(password)
                .contentTemplate(content)
                .build();
    }
}
