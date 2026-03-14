package com.kuafuai.dynamic.validation;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Lists;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.I18nUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.helper.DynamicCheckValue;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;
import java.util.Map;

public class ColumnValueChecker {

    public static void normalizeAndValidate(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {

        DynamicCheckValue.check_table_column_default_value(columns, conditions);
        DynamicCheckValue.check_table_column_value(columns, conditions);
    }

    /**
     * 更新时 - 对数据进行类型判断与是否超出范围
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
                // 如果 value 是 Map，说明是修改条件
                continue;
            }

            switch (dslType) {
                case "number":
                case "quote":
                case "int":
                    validateNumeric(value, columnName, table);
                    break;
                case "date":
                    convertAndValidateDate(value, columnName, Lists.newArrayList("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"), table, conditions);
                    break;
                case "datetime":
                    convertAndValidateDate(value, columnName, Lists.newArrayList("yyyy-MM-dd HH:mm:ss"), table, conditions);
                    break;
                case "time":
                    convertAndValidateDate(value, columnName, Lists.newArrayList("HH:mm:ss"), table, conditions);
                    break;
                case "boolean":
                    conditions.put(columnName, Convert.toBool(value, false));
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

    private static void convertAndValidateDate(Object value, String columnName,
                                               List<String> formats, String table, Map<String, Object> conditions) {

        String timeStr = Convert.toStr(value);

        if (StringUtils.isEmpty(timeStr)) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columnName));
        }

        DateTime dateTime = null;

        try {
            // 1 常规解析
            dateTime = DateUtil.parse(timeStr);
        } catch (Exception ignored) {
        }

        if (dateTime == null) {
            try {
                // 2 JS Date 解析
                dateTime = DateUtil.parse(timeStr, "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", java.util.Locale.US);
            } catch (Exception ignored) {
            }
        }

        if (dateTime == null) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columnName));
        }

        String formattedDate = DateUtil.format(dateTime, formats.get(0));
        conditions.put(columnName, formattedDate);
    }

    private static void validateDecimal(Object value, String columName, String table) {
        String strValue = Convert.toStr(value);
        if (!NumberUtil.isNumber(strValue)) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columName));
        }
        try {
            double val = Double.parseDouble(strValue);
            if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                throw new BusinessException(I18nUtils.get("dynamic.update.value_out_range", table + ":" + columName));
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columName));
        }
    }

}
