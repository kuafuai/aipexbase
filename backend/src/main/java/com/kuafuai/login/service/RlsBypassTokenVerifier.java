package com.kuafuai.login.service;

import com.kuafuai.login.config.RlsBypassProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Slf4j
@Component
public class RlsBypassTokenVerifier {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final int TOKEN_HEX_LEN = 16;

    @Autowired
    private RlsBypassProperties properties;

    public boolean verify(String submitted) {
        long window = Instant.now().getEpochSecond() / properties.getWindowSeconds();
        int skew = properties.getSkew();
        byte[] submittedBytes = submitted.getBytes(StandardCharsets.UTF_8);
        for (int i = -skew; i <= skew; i++) {
            String expected = compute(window + i);
            if (constantTimeEquals(submittedBytes, expected.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    public String currentToken() {
        long window = Instant.now().getEpochSecond() / properties.getWindowSeconds();
        return compute(window);
    }

    private String compute(long window) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(properties.getSeed().getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] digest = mac.doFinal(Long.toString(window).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(TOKEN_HEX_LEN);
            for (int i = 0; i < TOKEN_HEX_LEN / 2; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("compute RLS bypass token failed", e);
            return "";
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
