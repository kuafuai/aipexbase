package com.kuafuai.dynamic.service;

import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.helper.ContextFactory;
import com.kuafuai.dynamic.orchestrator.*;

import java.util.Map;

public class DynamicSQLBuilder {

    public static String buildDynamicSelect(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new SelectSqlBuilder(context).buildPage();
    }

    public static String buildDynamicCount(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new SelectSqlBuilder(context).buildCount();
    }

    public static String buildDynamicList(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new SelectSqlBuilder(context).buildList();
    }

    public static String buildDynamicOne(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new SelectSqlBuilder(context).buildOne();
    }

    public static String buildDynamicInsert(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new InsertSqlBuilder(context).build();
    }

    public static String buildDynamicUpdate(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new UpdateSqlBuilder(context).build();
    }

    public static String buildDynamicDelete(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new DeleteSqlBuilder(context).build();
    }

    public static String buildDynamicStatisticsCount(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return new StatisticsSqlBuilder(context).build();
    }
}
