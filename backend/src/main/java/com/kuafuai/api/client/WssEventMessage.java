package com.kuafuai.api.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WssEventMessage {
    private boolean flag;
    private String content;
}
