package com.kuafuai.voice.gateway;

import lombok.Value;

@Value
public class TokenUsage {
    public static final TokenUsage ZERO = new TokenUsage(0, 0, 0);

    int promptTokens;
    int completionTokens;
    int totalTokens;
}
