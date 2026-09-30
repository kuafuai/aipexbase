package com.kuafuai.voice.gateway;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * 火山 ASR/TTS WebSocket 帧解析器。
 *
 * <p>协议格式参照 body 侧 {@code HuoshanAsr.kt} 和 {@code HuoshanTts.kt}：
 * <pre>
 * byte[0] = (proto_ver << 4) | header_size
 * byte[1] = (message_type << 4) | flags
 * byte[2] = (serialization << 4) | compression
 * byte[3] = reserved
 * [seq(4B) if flags & 0x01]
 * [event(4B) if flags & 0x04]
 * payloadSize(4B) + payload
 * </pre>
 *
 * <p>只识别两种关键帧：
 * <ul>
 *   <li>ASR final response (messageType=0x9, isLast=1, 无 event)：获取音频时长
 *   <li>TTS SessionFinished (messageType=0x9, event=152)：获取输入字符数
 * </ul>
 */
@Slf4j
public class HuoshanFrameParser {

    private static final int MSG_SERVER_FULL_RESPONSE = 0x9;
    private static final int FLAG_HAS_SEQ = 0x01;
    private static final int FLAG_IS_LAST = 0x02;
    private static final int FLAG_HAS_EVENT = 0x04;
    private static final int COMPRESSION_GZIP = 0x1;

    private static final int EVENT_SESSION_FINISHED = 152;

    /**
     * connection 级事件不带 sessionId，session 级事件带。参照 HuoshanTts.kt isConnectionEvent。
     */
    private static boolean isConnectionEvent(int event) {
        return event == 1     // StartConnection
                || event == 2 // FinishConnection
                || event == 50 // ConnectionStarted
                || event == 51 // ConnectionFailed
                || event == 52; // ConnectionFinished
    }

    /**
     * 判帧一次，命中就返回结果。TTS/ASR 走同一个入口，避免调用方 double dispatch。
     *
     * @param frame 完整的 WebSocket 二进制帧
     * @return 命中 ASR final 或 TTS SessionFinished 时返回结果；否则 null
     */
    public static VoiceUsage tryDecode(byte[] frame) {
        if (frame == null || frame.length < 4) return null;

        // byte[1] = (message_type << 4) | flags
        int byte1 = frame[1] & 0xFF;
        int messageType = (byte1 >> 4) & 0x0F;
        int flags = byte1 & 0x0F;

        // 我们要的两种都在 MSG_SERVER_FULL_RESPONSE 下
        if (messageType != MSG_SERVER_FULL_RESPONSE) return null;

        int byte2 = frame[2] & 0xFF;
        int compression = byte2 & 0x0F;

        boolean hasSeq = (flags & FLAG_HAS_SEQ) != 0;
        boolean isLast = (flags & FLAG_IS_LAST) != 0;
        boolean hasEvent = (flags & FLAG_HAS_EVENT) != 0;

        // 计算 payload 起始位置 & 读出 event（如果有）
        int offset = 4;
        if (hasSeq) offset += 4;
        int event = 0;
        if (hasEvent) {
            if (frame.length < offset + 4) return null;
            event = readInt32BE(frame, offset);
            offset += 4;
        }

        // 判定 tag：
        // - TTS SessionFinished：hasEvent && event=152
        // - ASR final：isLast && !hasEvent（火山 ASR 不用 event 通道）
        String tag;
        if (hasEvent && event == EVENT_SESSION_FINISHED) {
            tag = VoiceUsage.TAG_TTS;
        } else if (isLast && !hasEvent) {
            tag = VoiceUsage.TAG_ASR;
        } else {
            return null;
        }

        // TTS session 级事件在 event 之后还有 sessionId 字段：size(4B) + bytes
        // 参照 body 侧 HuoshanTts.kt buildClientEventFrame / parseMessage 的逻辑
        if (hasEvent && !isConnectionEvent(event)) {
            if (frame.length < offset + 4) return null;
            int sidSize = readInt32BE(frame, offset);
            offset += 4;
            if (frame.length < offset + sidSize) return null;
            offset += sidSize;
        }

        if (frame.length < offset + 4) return null;
        int payloadSize = readInt32BE(frame, offset);
        offset += 4;
        if (frame.length < offset + payloadSize) return null;

        byte[] payload = new byte[payloadSize];
        System.arraycopy(frame, offset, payload, 0, payloadSize);
        if (compression == COMPRESSION_GZIP) {
            payload = gunzip(payload);
            if (payload == null) return null;
        }

        String json = new String(payload, StandardCharsets.UTF_8);
        int durationMs = 0;
        int textWords = 0;
        if (VoiceUsage.TAG_ASR.equals(tag)) {
            durationMs = extractInt(json, "\"audio_info\"", "\"duration\"");
        } else {
            textWords = extractInt(json, "\"usage\"", "\"text_words\"");
        }
        return new VoiceUsage(tag, json, durationMs, textWords);
    }

    /**
     * 从 JSON 字符串里定位 outerKey 对象内的 innerKey 数值。
     * 不用 JSON 库是为了避免每帧引入一次解析开销；ASR 和 TTS 的字段结构固定，字符串
     * 匹配足够。找不到返回 0。
     */
    private static int extractInt(String json, String outerKey, String innerKey) {
        int outer = json.indexOf(outerKey);
        if (outer < 0) return 0;
        int inner = json.indexOf(innerKey, outer);
        if (inner < 0) return 0;
        int colon = json.indexOf(':', inner + innerKey.length());
        if (colon < 0) return 0;
        int i = colon + 1;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\t')) i++;
        int start = i;
        while (i < json.length() && Character.isDigit(json.charAt(i))) i++;
        if (i == start) return 0;
        try {
            return Integer.parseInt(json.substring(start, i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int readInt32BE(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) << 24)
                | ((buf[offset + 1] & 0xFF) << 16)
                | ((buf[offset + 2] & 0xFF) << 8)
                | (buf[offset + 3] & 0xFF);
    }

    private static byte[] gunzip(byte[] compressed) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
             GZIPInputStream gzip = new GZIPInputStream(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = gzip.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Throwable t) {
            log.warn("gunzip failed: {}", t.toString());
            return null;
        }
    }
}
