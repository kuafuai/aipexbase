package com.kuafuai.dynamic.context;

import lombok.Value;

@Value
public class SqlPart {
    String text;

    public static SqlPart of(String s) {
        return new SqlPart(s == null ? "" : s);
    }

    public boolean isEmpty() {
        return text == null || text.trim().isEmpty();
    }
}
