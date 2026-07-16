package com.kuafuai.manage.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of executing a dynamic API test call.
 *
 * The entire request/response cycle is captured so the dashboard can render
 * a Postman-like debug panel: what was sent (rendered), what came back,
 * how long it took.
 *
 * NOTE: A non-2xx {@code status} is still considered a successful test —
 * the test's job is to reveal what the target API returned. Only when
 * the request could not be dispatched at all (DNS failure, timeout, ...)
 * do we set {@code error}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicApiTestResultVO {

    // ---------- Request that was actually sent ----------
    private String requestUrl;         // final URL after template render
    private String requestMethod;
    private Map<String, String> requestHeaders;
    private String requestBody;        // rendered body, may be null for GET

    // ---------- Response from the target ----------
    private Integer status;            // e.g. 200, 404, 500. null if network failed
    private String statusText;
    private Map<String, String> responseHeaders;
    private String body;               // raw response body as string
    private Object bodyJson;           // parsed JSON if applicable, else null

    // ---------- Metrics ----------
    private Long timeMs;               // wall-clock end-to-end
    private Long sizeBytes;            // response body size

    // ---------- Failure info (only set on transport-level failure) ----------
    private String error;              // e.g. "connect timeout", "DNS failure"
}
