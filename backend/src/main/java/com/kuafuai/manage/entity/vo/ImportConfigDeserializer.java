package com.kuafuai.manage.entity.vo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * 支持 importConfig 为 JSON 对象，或外层被序列化成字符串的一段 JSON（部分网关/客户端会这样传）。
 */
public class ImportConfigDeserializer extends JsonDeserializer<Map<String, Object>> {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getValueAsString();
            if (s == null || s.isEmpty()) {
                return null;
            }
            return mapper.readValue(s, MAP_TYPE);
        }
        if (t == JsonToken.START_OBJECT) {
            return mapper.readValue(p, MAP_TYPE);
        }
        return null;
    }
}
