package com.kuafuai.manage.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Key view returned exactly once, at creation time.
 *
 * WARNING: The {@code keyName} field contains the FULL PLAINTEXT key.
 * This is the only response shape where the plaintext is exposed —
 * subsequent list/detail calls return {@link APIKeyMaskedVO} instead.
 *
 * The frontend must show it to the user immediately and instruct them
 * to save it, because the value cannot be retrieved again.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIKeyCreatedVO {

    private Long id;

    private String appId;

    private String name;

    /** Full plaintext key — only ever returned by the create endpoint. */
    private String keyName;

    private String description;

    private String status;

    private String createAt;

    private String expireAt;
}
