package com.kuafuai.dynamic.validation;

import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Lists;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.I18nUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.helper.DynamicCheckValue;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.time.LocalDateTime;
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

    private static void convertAndValidateDate(Object value, String columName,
                                               List<String> formats, String table, Map<String, Object> conditions) {

        String timeStr = Convert.toStr(value);

        if (StringUtils.isEmpty(timeStr)) {
            throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columName));
        }

        // 0、直接支持 java.util.Date 对象
        if (value instanceof java.util.Date) {
            java.util.Date date = (java.util.Date) value;
            LocalDateTime dateTime = date.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            String formattedDate = dateTime.format(DateTimeFormatter.ofPattern(formats.get(0)));
            conditions.put(columName, formattedDate);
            return;
        }

        // 1、直接支持 LocalDateTime 对象
        if (value instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) value;
            String formattedDate = dateTime.format(DateTimeFormatter.ofPattern(formats.get(0)));
            conditions.put(columName, formattedDate);
            return;
        }

        // 2、兼容 ISO8601 时间格式 (2026-03-13T09:04:25.000Z)
        try {
            if (timeStr.contains("T")) {
                java.time.OffsetDateTime offsetDateTime = java.time.OffsetDateTime.parse(timeStr);
                java.time.LocalDateTime dateTime = offsetDateTime.toLocalDateTime();

                String formattedDate = dateTime.format(DateTimeFormatter.ofPattern(formats.get(0)));
                conditions.put(columName, formattedDate);
                return;
            }
        } catch (Exception ignored) {}

        // 3、兼容 JavaScript Date.toString() 格式 (Sat Mar 14 2026 12:30:45 GMT+0800)
        try {
            if (timeStr.matches("^[A-Z][a-z]{2} [A-Z][a-z]{2} .*GMT.*$")) {
                // 尝试多种可能的格式
                java.text.SimpleDateFormat[] jsFormats = {
                    new java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", java.util.Locale.US),
                    new java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", java.util.Locale.US),
                    new java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'XXX", java.util.Locale.US)
                };

                for (java.text.SimpleDateFormat format : jsFormats) {
                    try {
                        java.util.Date date = format.parse(timeStr);
                        java.time.LocalDateTime dateTime =
                                date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

                        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern(formats.get(0)));
                        conditions.put(columName, formattedDate);
                        return;
                    } catch (Exception e) {
                        // 继续尝试下一个格式
                    }
                }
            }
        } catch (Exception ignored) {}

        // 4、兼容时间戳 (毫秒)
        if (NumberUtil.isNumber(timeStr) && timeStr.length() >= 10) {
            try {
                long timestamp = Long.parseLong(timeStr);
                if (timeStr.length() == 10) {
                    timestamp = timestamp * 1000;
                }

                java.time.LocalDateTime dateTime =
                        java.time.Instant.ofEpochMilli(timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime();

                String formattedDate = dateTime.format(DateTimeFormatter.ofPattern(formats.get(0)));
                conditions.put(columName, formattedDate);
                return;
            } catch (Exception ignored) {}
        }

        // 5、原有格式匹配
        for (String format : formats) {
            try {
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern(format).withResolverStyle(ResolverStyle.STRICT);

                java.time.temporal.TemporalAccessor parsed = formatter.parse(timeStr);

                if (format.equals("yyyy-MM-dd")) {
                    java.time.LocalDate date = java.time.LocalDate.from(parsed);
                    conditions.put(columName, date.format(formatter));
                } else if (format.equals("yyyy-MM-dd HH:mm:ss")) {
                    java.time.LocalDateTime dateTime = java.time.LocalDateTime.from(parsed);
                    conditions.put(columName, dateTime.format(formatter));
                } else if (format.equals("HH:mm:ss")) {
                    java.time.LocalTime time = java.time.LocalTime.from(parsed);
                    conditions.put(columName, time.format(formatter));
                }
                return;
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new BusinessException(I18nUtils.get("dynamic.update.value_type_error", table + ":" + columName));
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
