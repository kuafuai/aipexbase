package com.kuafuai.dynamic.validation;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;
import java.util.Map;

public class RequiredColumnChecker {

    public static void check(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {
        for (AppTableColumnInfo columnInfo : columns) {
            if (!columnInfo.isPrimary() && !columnInfo.isNullable()) {
                String col = columnInfo.getColumnName();
                if (!conditions.containsKey(col) || conditions.get(col) == null) {
                    throw new BusinessException(columnInfo.getAppId() + ":字段:" + col + "的值不能为空");
                }
            }
        }

    }
}
