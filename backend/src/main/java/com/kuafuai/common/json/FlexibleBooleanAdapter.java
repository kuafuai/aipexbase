package com.kuafuai.common.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.kuafuai.common.text.Convert;

import java.io.IOException;

public class FlexibleBooleanAdapter extends TypeAdapter<Boolean> {
    @Override
    public void write(JsonWriter jsonWriter, Boolean aBoolean) throws IOException {
        jsonWriter.value(aBoolean ? 1 : 0); // 输出为 1/0
    }

    @Override
    public Boolean read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BOOLEAN:
                return in.nextBoolean();
            case NUMBER:
                return in.nextInt() != 0;
            case STRING:
                String str = in.nextString().toLowerCase();
                return Convert.toBool(str);
            case NULL:
                in.nextNull();
                return false;
            default:
                throw new IllegalStateException("Unexpected token: " + token);
        }
    }
}
