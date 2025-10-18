package com.kuafuai.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.common.json.FlexibleBooleanAdapter;

import java.lang.reflect.Type;
import java.util.List;

public class JSON {

    private static JSON instance = new JSON();
    private Gson gson;

    private JSON() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Boolean.class, new FlexibleBooleanAdapter())
                .registerTypeAdapter(boolean.class, new FlexibleBooleanAdapter())
                .create();
    }

    public static String toJSONString(Object obj) {
        return instance.toJson(obj);
    }

    public static <T> T parseObject(String value, Class<T> clazz) {
        return instance.fromJson(value, clazz);
    }

    public static <T> T parseObject(String value, Type type) {
        return instance.fromJson(value, type);
    }

    public static <T> List<T> parseArray(JsonElement element) {
        Type type = new TypeToken<List<T>>() {
        }.getType();
        return instance.fromJson(element, type);
    }

    public static <T> List<T> parseArray(String value, Class<T> clazz) {
        Type type = new TypeToken<List<T>>() {
        }.getType();
        return instance.fromJson(value, type);
    }

    public static <T> List<T> parseArray(String value, Type type) {
        return instance.fromJson(value, type);
    }

    public String toJson(Object obj) {
        return this.gson.toJson(obj);
    }

    public <T> T fromJson(String value, Class<T> clazz) {
        return this.gson.fromJson(value, clazz);
    }

    public <T> T fromJson(JsonElement element, Type typeOf) {
        return this.gson.fromJson(element, typeOf);
    }

    public <T> T fromJson(String element, Type typeOf) {
        return this.gson.fromJson(element, typeOf);
    }


}
