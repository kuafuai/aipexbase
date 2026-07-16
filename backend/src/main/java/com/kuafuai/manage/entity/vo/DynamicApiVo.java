package com.kuafuai.manage.entity.vo;


import lombok.Data;

@Data
public class DynamicApiVo {
    private String keyName;
    private String appId;
    private String description;
    private String url;
    private String method;

    /** JSON | Form | Text — determines how the runtime encodes the request body */
    private String bodyType;

    private String bodyTemplate;
    private String headerTemplate;

    /**
     * Bearer/API token used at runtime. Stored in plain text for now — should
     * be encrypted at rest in a future iteration.
     */
    private String token;

    /**
     * JSONPath used by the runtime to extract the useful part of the response.
     * Example: "$.data.result"
     */
    private String dataPath;

    /**
     * Expected shape of the extracted data: TEXT | JSON | ARRAY.
     * Helps the runtime and downstream AI-generated code know how to consume it.
     */
    private String dataType;

    /**
     * Optional response example (raw text). Shown to AI Coding tools so they
     * can generate correct consumer code. Runtime does not read this.
     */
    private String dataRaw;

    private String vars;
}
