package com.kuafuai.dynamic.service;

import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.helper.ContextFactory;
import com.kuafuai.dynamic.helper.SqlGuard;
import com.kuafuai.dynamic.orchestrator.*;

import java.util.Map;

public class DynamicSQLBuilder {

    public static String buildDynamicSelect(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new SelectSqlBuilder(context).buildPage());
    }

    public static String buildDynamicCount(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new SelectSqlBuilder(context).buildCount());
    }

    public static String buildDynamicList(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new SelectSqlBuilder(context).buildList());
    }

    public static String buildDynamicOne(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new SelectSqlBuilder(context).buildOne());
    }

    public static String buildDynamicInsert(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new InsertSqlBuilder(context).build());
    }

    public static String buildDynamicUpdate(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new UpdateSqlBuilder(context).build());
    }

    public static String buildDynamicDelete(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new DeleteSqlBuilder(context).build());
    }

    public static String buildDynamicStatisticsCount(Map<String, Object> params) {
        TableContext context = ContextFactory.fromParams(params);
        return SqlGuard.check(new StatisticsSqlBuilder(context).build());
    }
}
