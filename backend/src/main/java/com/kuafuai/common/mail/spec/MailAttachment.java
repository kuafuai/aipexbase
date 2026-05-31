package com.kuafuai.common.mail.spec;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailAttachment {
    private String name;
    private String contentType;
    private int size;
}
