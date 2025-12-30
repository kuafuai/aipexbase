package com.kuafuai.dynamic.validation;

import cn.hutool.core.util.NumberUtil;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.DateUtils;
import com.kuafuai.common.util.I18nUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.helper.DynamicCheckValue;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;

public class ColumnValueChecker {

    public static void normalizeAndValidate(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {

        DynamicCheckValue.check_table_column_default_value(columns, conditions);
        DynamicCheckValue.check_table_column_value(columns, conditions);
    }

    /**
     * 跟新时-对数据进行类型判断与是否超出范围
     */
    public static void normalizeByUpdateValidate(String table, List<AppTableColumnInfo> columns, Map<String, Object> conditions) {

        for (AppTableColumnInfo columnInfo : columns) {
            if (columnInfo.isPrimary()) {
                continue;
            }

            String columnName = columnInfo.getColumnName();
            if (!conditions.containsKey(columnName)) {
                continue;
            }

            String dslType = StringUtils.lowerCase(columnInfo.getDslType());
            String columnType = StringUtils.lowerCase(columnInfo.getColumnType());
            Object value = conditions.get(columnName);

            if (value == null) {
                continue;
            }

            if (value instanceof Map) {
                // 如果value是Map，说明是修改条件
                continue;
            }

            switch (dslType) {
                case "number":
                case "quote":
                case "int":
                    validateNumeric(value, columnName, table);
                    break;
                case "date":
                    validateDate(value, columnName, "yyyy-MM-dd", table);
                    break;
                case "datetime":
                    validateDate(value, columnName, "yyyy-MM-dd HH:mm:ss", table);
                    break;
                case "time":
                    validateDate(value, columnName, "HH:mm:ss", table);
                    break;
                case "decimal":
                case "float":
                case "double":
                    validateDecimal(value, columnName, table);
                    break;
                default:
                    if ("int".equals(columnType)) {
                        validateNumeric(value, columnName, table);
                    }
                    break;
            }
        }
    }

    private static void validateNumeric(Object value, String columName, String table) {
        String strValue = Convert.toStr(value);
        if (!NumberUtil.isNumber(strValue)) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columName));
        }
        try {
            long valueLong = Long.parseLong(strValue);
            if (valueLong > Integer.MAX_VALUE || valueLong < Integer.MIN_VALUE) {
                throw new BusinessException(I18nUtils.get("dynamic.update.value_out_range", table + ":" + columName));
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columName));
        }
    }

    private static void validateDate(Object value, String columName, String format, String table) {
        try {
            String timeStr = Convert.toStr(value);
            if (StringUtils.isEmpty(timeStr)) {
                throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", columName));
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withResolverStyle(ResolverStyle.STRICT);
            formatter.parse(timeStr);
        } catch (DateTimeParseException e) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", columName));
        }
    }

    private static void validateDecimal(Object value, String columName, String table) {
        String strValue = Convert.toStr(value);
        if (!NumberUtil.isNumber(strValue)) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", columName));
        }
        try {
            double val = Double.parseDouble(strValue);
            if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                throw new BusinessException(I18nUtils.get("dynamic.update.value_out_range", columName));
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", columName));
        }
    }

}
