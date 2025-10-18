package com.kuafuai.manage.mcp.util;

import java.util.HashMap;
import java.util.Map;

public class SchemaBuilder {
    private final Map<String, Object> properties = new HashMap<>();

    public static SchemaBuilder create() {
        return new SchemaBuilder();
    }

    public SchemaBuilder addProperty(String name, String type, String description) {
        Map<String, Object> json = new HashMap<>();
        json.put("type", type);
        json.put("description", description);
        properties.put(name, json);
        return this;
    }

//    public SchemaBuilder addProperty(String name, String type, String description, List<String> enumValues) {
//        Map<String, Object> property = new HashMap<>();
//        property.put("type", type);
//        property.put("description", description);
//        if (enumValues != null) {
//            property.put("enum", enumValues);
//        }
//        properties.put(name, property);
//        return this;
//    }

    public Map<String, Object> build() {
        Map<String, Object> result = new HashMap<>();
        result.put("properties", properties);
        return result;
    }
}
