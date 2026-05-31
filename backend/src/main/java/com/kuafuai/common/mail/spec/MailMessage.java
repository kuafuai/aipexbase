package com.kuafuai.common.mail.spec;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class MailMessage {
    private long uid;
    private String messageId;
    private String from;
    private List<String> to;
    private List<String> cc;
    private String subject;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sentDate;
    private String body;
    private List<MailAttachment> attachments;

    @Override
    public String toString() {
        return "MailMessage{" +
                "uid=" + uid +
                ", messageId='" + messageId + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}
