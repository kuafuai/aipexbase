package com.kuafuai.voice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "voice.huoshan")
public class HuoshanProperties {

    /** 火山真实 X-Api-Key。网关持有，客户端永远拿不到。 */
    private String apiKey;

    /** ASR 上游基础 URL，路径由 /voice/asr/** 之后的部分拼上去。 */
    private String asrBase = "wss://openspeech.bytedance.com/api/v3/sauc";

    /** TTS 上游基础 URL。 */
    private String ttsBase = "wss://openspeech.bytedance.com/api/v3/tts";

    /** 允许客户端使用的 ASR X-Api-Resource-Id 白名单。空表示不校验。 */
    private List<String> allowedAsrResources = Collections.singletonList("volc.bigasr.sauc.duration");

    /** 允许客户端使用的 TTS X-Api-Resource-Id 白名单。空表示不校验。 */
    private List<String> allowedTtsResources = Collections.singletonList("seed-tts-2.0");
}
