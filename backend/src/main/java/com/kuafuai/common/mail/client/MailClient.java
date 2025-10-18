package com.kuafuai.common.mail.client;

import com.kuafuai.api.util.ApiUtil;
import com.kuafuai.common.mail.spec.MailDefinition;
import org.springframework.mail.MailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

public class MailClient {

    public void send(MailDefinition definition, String to, String subject, Map<String, String> params) {
        JavaMailSenderImpl sender = createSender(definition);
        MimeMessage message = createMessage(sender, definition, to, subject, params);
        if (message != null) {
            sender.send(message);
        }
    }

    private JavaMailSenderImpl createSender(MailDefinition definition) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // 邮箱服务器配置
        mailSender.setHost(definition.getHost());
        mailSender.setPort(definition.getPort());
        mailSender.setUsername(definition.getUserName());
        mailSender.setPassword(definition.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true"); // 开启 SSL

        return mailSender;
    }

    private MimeMessage createMessage(JavaMailSenderImpl mailSender, MailDefinition definition, String to, String subject, Map<String, String> params) {
        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(definition.getUserName()); // 必须和配置里的用户名一致
            helper.setTo(to);
            helper.setSubject(subject);
            String content = ApiUtil.interpolateString(definition.getContentTemplate(), params);
            helper.setText(content, true);

            return message;
        } catch (MessagingException ignored) {
            return null;
        }
    }
}
