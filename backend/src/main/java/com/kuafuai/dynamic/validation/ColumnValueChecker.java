package com.kuafuai.dynamic.validation;

import com.kuafuai.dynamic.helper.DynamicCheckValue;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;
import java.util.Map;

public class ColumnValueChecker {

    public static void normalizeAndValidate(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {
        
        DynamicCheckValue.check_table_column_default_value(columns, conditions);
        DynamicCheckValue.check_table_column_value(columns, conditions);
    }
}
