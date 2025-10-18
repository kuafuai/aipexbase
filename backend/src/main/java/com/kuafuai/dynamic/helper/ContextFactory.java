package com.kuafuai.dynamic.helper;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.dynamic.context.Params;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;
import java.util.Map;

public class ContextFactory {

    public static TableContext fromParams(Map<String, Object> params) {
        String db = Params.getDatabase(params);
        String tb = Params.getTable(params);
        Map<String, Object> conditions = Params.getConditions(params);

        checkDatabaseAndTable(db, tb);
        List<AppTableColumnInfo> columns = getTableColumns(db, tb);

        return TableContext.builder()
                .database(db)
                .table(tb)
                .conditions(conditions)
                .columns(columns)
                .build();
    }

    private static void checkDatabaseAndTable(String database, String table) {
        DynamicInfoCache cache = SpringUtils.getBean(DynamicInfoCache.class);
        if (cache.getAppTableInfo(database, table) == null) {
            throw new BusinessException(database + ":" + table + ":数据不存在====>表不存在");
        }
    }

    private static List<AppTableColumnInfo> getTableColumns(String database, String table) {
        DynamicInfoCache cache = SpringUtils.getBean(DynamicInfoCache.class);
        return cache.getAppTableColumnInfo(database, table);
    }
}
