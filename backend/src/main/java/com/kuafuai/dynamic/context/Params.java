package com.kuafuai.dynamic.context;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Params {

    public static final String DATABASE_KEY = "database";
    public static final String TABLE_KEY = "table";
    public static final String CONDITIONS_KEY = "conditions";
    public static final String OFFSET = "offset";
    public static final String LIMIT = "limit";

    public static String getDatabase(Map<String, Object> params) {
        return String.valueOf(params.get(DATABASE_KEY));
    }

    public static String getTable(Map<String, Object> params) {
        return String.valueOf(params.get(TABLE_KEY));
    }

    public static Map<String, Object> getConditions(Map<String, Object> params) {
        Object obj = params.get(CONDITIONS_KEY);
        if (obj instanceof Map)
            return (Map<String, Object>) obj;

        Map<String, Object> empty = new HashMap<>();
        params.put(CONDITIONS_KEY, empty);
        return empty;
    }
}
