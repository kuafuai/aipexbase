package com.kuafuai.manage.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Masked API Key view for list responses.
 *
 * The raw key (kf_api_XXXX...XXXX) is never returned by the list endpoint.
 * Only the prefix (first 11 chars = "kf_api_" + 4) and suffix (last 4 chars)
 * are kept in the middle is replaced with bullets.
 *
 * The full plaintext key is only returned once at creation time via
 * {@link APIKeyCreatedVO}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIKeyMaskedVO {

    private Long id;

    private String appId;

    private String name;

    /** Masked, e.g. "kf_api_1a2b••••••••••••••••••••••2d3f" */
    private String keyName;

    private String description;

    private String status;

    private String createAt;

    private String lastUsedAt;

    private String expireAt;

    /**
     * Mask helper: keep 11-char prefix ("kf_api_" + 4 chars) and 4-char suffix,
     * bullets in the middle. Falls back to raw value if too short.
     */
    public static String mask(String rawKey) {
        if (rawKey == null || rawKey.length() <= 16) {
            return rawKey;
        }
        String prefix = rawKey.substring(0, 11);
        String suffix = rawKey.substring(rawKey.length() - 4);
        return prefix + "••••••••••••••••••••••" + suffix;
    }
}
