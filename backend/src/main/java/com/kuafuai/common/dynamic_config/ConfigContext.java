package com.kuafuai.common.dynamic_config;

import com.kuafuai.login.handle.GlobalAppIdFilter;

public class ConfigContext {

    private static ThreadLocal<String> database = new ThreadLocal<>();


    public static void setDatabase(String database) {
        ConfigContext.database.set(database);
    }

    public static String getDatabase() {
        final String appId = GlobalAppIdFilter.getAppId();
        if (appId != null){
            return appId;
        }
        return database.get();
    }

    public static void clear() {
        database.remove();
    }



}
