package com.kuafuai.manage.entity.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class ApiDocumentParsedVo {
    private BasicInfo basic_info;
    private ApiConfig api_config;
    private BillingInfo billing_info;
    private ResponseConfig response_config;
    private List<VariableConfig> variables_config;

    @Data
    public static class BasicInfo {
        private String api_name;
        private String api_description;
        private String category;
        private Boolean enabled;
    }

    @Data
    public static class ApiConfig {
        private String request_url;
        private String protocol;
        private String method;
        private AuthConfig auth_config;
        private RequestBody request_body;

        @Data
        public static class AuthConfig {
            private String auth_type;
            private String token;
        }

        @Data
        public static class RequestBody {
            private String body_type;
            private String body_template;
        }
    }

    @Data
    public static class BillingInfo {
        private String pricing_model;
        private Boolean is_billing;
    }

    @Data
    public static class ResponseConfig {
        private String data_path;
        private String data_type;
        private JsonNode data_row;
    }

    @Data
    public static class VariableConfig {
        private String name;
        private String description;
        private Boolean required;
        private String default_value;
        private String location;
    }
}