package com.kuafuai.dynamic.validation;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.I18nUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.helper.DynamicCheckValue;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
                case "int":
                    validateNumeric(value, columnName, table);
                    break;

                case "date":
                    convertAndValidateDate(value, columnName, "yyyy-MM-dd", table, conditions);
                    break;

                case "datetime":
                    convertAndValidateDate(value, columnName, "yyyy-MM-dd HH:mm:ss", table, conditions);
                    break;

                case "time":
                    convertAndValidateDate(value, columnName, "HH:mm:ss", table, conditions);
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

    /**
     * 日期解析与格式化（只传最终存储格式）
     */
    private static void convertAndValidateDate(Object value,
                                               String columnName,
                                               String finalFormat,
                                               String table,
                                               Map<String, Object> conditions) {

        String timeStr = Convert.toStr(value);

        if (StringUtils.isEmpty(timeStr)) {
            throw new BusinessException(
                    I18nUtils.get("dynamic.update.value_type_error", table + ":" + columnName)
            );
        }

        // 固定解析候选格式
        List<String> parseFormats = Arrays.asList(
                "",  // 自动解析
                "EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
        );

        DateTime dateTime = tryParseDate(timeStr, parseFormats);

        if (dateTime == null) {
            throw new BusinessException(
                    I18nUtils.get("dynamic.update.value_type_error", table + ":" + columnName)
            );
        }

        // 最终存储格式
        conditions.put(columnName, DateUtil.format(dateTime, finalFormat));
    }

    /**
     * 日期解析策略
     */
    private static DateTime tryParseDate(String timeStr, List<String> formats) {

        for (String format : formats) {
            try {
                // 判断空字符串使用自动解析
                if (StringUtils.isEmpty(format)) {
                    return DateUtil.parse(timeStr);
                } else {
                    return DateUtil.parse(timeStr, format, Locale.US);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
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
