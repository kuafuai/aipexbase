package com.kuafuai.system.entity.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TokenUsageReportRequest {
    @JsonProperty("user_id")
    private String codeFlyingUserId;

    @JsonProperty("chat_id")
    private String chatId;

    private List<RecordItem> records;

    @Data
    public static class RecordItem {

        @JsonProperty("message_id")
        private Long messageId;

        @JsonProperty("agent_id")
        private String agentId;

        private String model;

        private String timestamp;

        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        @JsonProperty("cached_tokens")
        private Integer cachedTokens;
    }
}
