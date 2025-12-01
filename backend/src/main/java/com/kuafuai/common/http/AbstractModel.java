package com.kuafuai.common.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractModel {
    public Map<String, String> header = new HashMap<String, String>();
    protected boolean skipSign = false;

    private HashMap<String, Object> customizedParams = new HashMap<String, Object>();

    public static <O extends AbstractModel> String toJsonString(O obj) {
        return toJsonObject(obj).toString();
    }


    private static <O extends AbstractModel> JsonObject toJsonObject(O obj) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        JsonObject joall = new JsonObject();
        JsonObject joadd = gson.toJsonTree(obj.any()).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : joadd.entrySet()) {
            joall.add(entry.getKey(), entry.getValue());
        }
        // jopublic will override joadd if key conflict exists
        JsonObject jopublic = gson.toJsonTree(obj).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jopublic.entrySet()) {
            Object fo = null;
            try {
                Field f = obj.getClass().getDeclaredField(entry.getKey());
                if (f == null) {
                    f = obj.getClass().getField(entry.getKey());
                }
                f.setAccessible(true);
                fo = f.get(obj);
            } catch (Exception e) {
                // this should never happen

            }
            if (fo instanceof AbstractModel) {
                joall.add(entry.getKey(), toJsonObject((AbstractModel) fo));
            } else {
                joall.add(entry.getKey(), entry.getValue());
            }
        }
        return joall;
    }

    public static <O> O fromJsonString(String json, Class<O> cls) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.fromJson(json, cls);
    }

    protected abstract void toMap(HashMap<String, String> map, String prefix);


    protected String[] getBinaryParams() {
        return new String[0];
    }


    protected HashMap<String, byte[]> getMultipartRequestParams() {
        return new HashMap<String, byte[]>();
    }

    protected <V> void setParamSimple(HashMap<String, String> map, String key, V value) {
        if (value != null) {

            // key = key.substring(0, 1).toUpperCase() + key.substring(1);
            // key = key.replace("_", ".");
            map.put(key, String.valueOf(value));
        }
    }

    protected <V> void setParamArraySimple(HashMap<String, String> map, String prefix, V[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                this.setParamSimple(map, prefix + i, array[i]);
            }
        }
    }

    protected <V extends AbstractModel> void setParamObj(HashMap<String, String> map, String prefix, V obj) {
        if (obj != null) {
            obj.toMap(map, prefix);
        }
    }

    protected <V extends AbstractModel> void setParamArrayObj(HashMap<String, String> map, String prefix, V[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                this.setParamObj(map, prefix + i + ".", array[i]);
            }
        }
    }

    /**
     * Set any key value pair to this model.
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        this.customizedParams.put(key, value);
    }

    /**
     * Get customized key value pairs from this model.
     */
    public HashMap<String, Object> any() {
        return this.customizedParams;
    }

    public boolean getSkipSign() {
        return skipSign;
    }

    public void setSkipSign(boolean skipSign) {
        this.skipSign = skipSign;
    }

    public Map<String, String> GetHeader() {
        return header;
    }

    public void setHeaderContentType(String value) {
        this.header.put("Content-Type", value);
    }

    public void SetHeader(Map<String, String> header) {
        this.header = header;
    }


}

