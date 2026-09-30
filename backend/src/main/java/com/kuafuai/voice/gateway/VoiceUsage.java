package com.kuafuai.voice.gateway;

import lombok.Value;

/**
 * ASR / TTS 一次会话的计量结果。由 {@link HuoshanFrameParser} 从火山最终帧里抽出。
 *
 * <p>tag 决定语义：
 * <ul>
 *   <li>{@code ASR_FINAL_PAYLOAD}：{@code audioDurationMs} 有值，{@code textWords}=0
 *   <li>{@code TTS_SESSION_FINISHED}：{@code textWords} 有值，{@code audioDurationMs}=0
 * </ul>
 * json 保留原始 payload，用于排查和后续可能想抽的其他字段。
 */
@Value
public class VoiceUsage {
    public static final String TAG_ASR = "ASR_FINAL_PAYLOAD";
    public static final String TAG_TTS = "TTS_SESSION_FINISHED";

    String tag;
    String json;
    /** ASR: 音频时长毫秒 (audio_info.duration)，非 ASR 帧为 0 */
    int audioDurationMs;
    /** TTS: 输入文本字符数 (usage.text_words)，非 TTS 帧为 0 */
    int textWords;
}
