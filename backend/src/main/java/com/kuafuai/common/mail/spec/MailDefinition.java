package com.kuafuai.common.mail.spec;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailDefinition {
    private String host;
    private Integer port;
    private String userName;
    private String password;
    private String contentTemplate;
}
