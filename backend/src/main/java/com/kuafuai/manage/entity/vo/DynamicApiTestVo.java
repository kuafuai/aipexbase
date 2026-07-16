package com.kuafuai.manage.entity.vo;


import lombok.Data;

import java.util.Map;

/**
 * Test request payload. All fields optional; used both for testing a stored
 * API (via /test/{keyName}) and for testing an inline unsaved config (via
 * /test-inline).
 */
@Data
public class DynamicApiTestVo {

    /**
     * Template variables to render into url / body / headers.
     * Example: {"userId": 123, "city": "Beijing"}
     */
    private Map<String, Object> params;

    /**
     * Optional headers to merge on top of the stored template.
     * Useful for temporary debugging without editing the saved API.
     */
    private Map<String, String> overrideHeaders;

    /**
     * Optional URL override (e.g., to hit a staging environment for the same API).
     */
    private String overrideUrl;

    // ---------- Inline config (used only by /test-inline) ----------
    // These let the caller test a config that hasn't been saved yet.

    private String url;
    private String method;
    private String bodyType;         // "template" | "form" | (default "template")
    private String bodyTemplate;
    private String headerTemplate;   // JSON string, e.g. {"X-Auth":"{{token}}"}
}
