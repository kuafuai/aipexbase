package com.kuafuai.common.handler;

public class DatabaseThreadLocal {
    public static final ThreadLocal<String> database = new ThreadLocal<>();

    public static void setDatabase(String database) {
        DatabaseThreadLocal.database.set(database);
    }

    public static String getDatabase() {
        return database.get();
    }

    public static void removeDatabase() {
        database.remove();
    }
}
