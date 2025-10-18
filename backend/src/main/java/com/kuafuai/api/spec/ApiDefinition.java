package com.kuafuai.api.spec;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiDefinition {
    public String name;
    public String method;
    public String url;
    public String headers;
    public String bodyType;
    public String bodyTemplate;
}
