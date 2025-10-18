package com.kuafuai.dynamic.helper;

import cn.hutool.core.util.NumberUtil;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.DateUtils;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppTableColumnInfo;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;

@Slf4j
public class DynamicCheckValue {

    /**
     * 检查 传入值 的 默认值
     *
     * @param columns    列
     * @param conditions cond
     */
    public static void check_table_column_default_value(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {
        for (AppTableColumnInfo columnInfo : columns) {
            String columnName = columnInfo.getColumnName();
            String dslType = StringUtils.lowerCase(columnInfo.getDslType());
            String columnType = StringUtils.lowerCase(columnInfo.getColumnType());

            if (!conditions.containsKey(columnName)) {
                continue;
            }

            if (columnInfo.isPrimary()) {
                continue;
            }

            String strValue = Convert.toStr(conditions.get(columnName));
            log.info("check:APP_ID:{}==============={}====={}", columnInfo.getAppId(), columnName, strValue);
            switch (dslType) {
                case "number":
                    setIfNotNumeric(conditions, columnName, 7);
                    break;
                case "quote":
                case "int":
                    setIfNotNumeric(conditions, columnName, 1);
                    break;
                case "boolean":
                    conditions.put(columnName, Convert.toBool(strValue, false));
                    break;
                case "date":
                    setIfInvalidDate(conditions, columnName, strValue, "yyyy-MM-dd", DateUtils.getTime());
                    break;
                case "datetime":
                    setIfInvalidDate(conditions, columnName, strValue, "yyyy-MM-dd HH:mm:ss", DateUtils.getTime());
                    break;
                case "time":
                    setIfInvalidDate(conditions, columnName, strValue, "HH:mm:ss", DateUtils.getHhMmSs());
                    break;
                case "decimal":
                case "float":
                case "double":
                    setIfNotDecimal(conditions, columnName, 1.0);
                    break;
                default:
                    if ("int".equals(columnType)) {
                        setIfNotNumeric(conditions, columnName, 0);
                    }
                    break;
            }
        }
    }

    /**
     * 检查数据类型 并 处理值
     *
     * @param columns    列
     * @param conditions cond
     */
    public static void check_table_column_value(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {
        for (AppTableColumnInfo columnInfo : columns) {
            String columnName = columnInfo.getColumnName();
            String dslType = columnInfo.getDslType();

            // 过滤掉资源类型
            if (StringUtils.equalsAnyIgnoreCase(dslType, "image", "images", "file", "files", "video", "videos")) {
                continue;
            }
            if (!conditions.containsKey(columnName)) {
                continue;
            }

            if (columnInfo.isPrimary()) {
                continue;
            }

            Object value = conditions.get(columnName);

            if (value instanceof Map) {
                logAndSend(columnInfo, columnName, "Map");
                conditions.put(columnName, processMapValue(columnName, (Map<?, ?>) value, columnInfo));

            } else if (value instanceof List) {
                logAndSend(columnInfo, columnName, "List");
                conditions.put(columnName, processListValue(columnName, (List<?>) value, columnInfo));

            } else {
                log.info("check:APP_ID:{} = {} ==== 原始值", columnInfo.getAppId(), columnName);
            }
        }
    }


    private static void logAndSend(AppTableColumnInfo columnInfo, String columnName, String type) {
        log.info("check:APP_ID:{} = {} ==== {}", columnInfo.getAppId(), columnName, type);
        sendMessage(columnInfo.getAppId(), "传入列: " + columnName + " 数据是" + type + ", 请跟进一下！！！");
    }

    private static void sendMessage(String appId, String message) {
        SystemBusinessService systemBusinessService = SpringUtils.getBean(SystemBusinessService.class);
        systemBusinessService.sendMessage(appId, message);
    }


    private static Object processMapValue(String columnName, Map<?, ?> map, AppTableColumnInfo columnInfo) {
        if (map.containsKey(columnName)) {
            log.info("check:APP_ID:{} = {} ==== Map.containsKey", columnInfo.getAppId(), columnName);
            return map.get(columnName);
        }
        if (map.size() == 1) {
            log.info("check:APP_ID:{} = {} ==== Map.size==1", columnInfo.getAppId(), columnName);
            return map.values().iterator().next();
        }
        log.info("check:APP_ID:{} = {} ==== Map.toJson", columnInfo.getAppId(), columnName);
        return JSON.toJSONString(map);
    }


    private static Object processListValue(String columnName, List<?> list, AppTableColumnInfo columnInfo) {
        if (list.isEmpty()) {
            log.info("check:APP_ID:{} = {} ==== List.size=0", columnInfo.getAppId(), columnName);
            return "0";
        }
        if (list.size() > 1) {
            log.info("check:APP_ID:{} = {} ==== List.toJson", columnInfo.getAppId(), columnName);
            return JSON.toJSONString(list);
        }

        // 只有一个元素
        Object first = list.get(0);
        if (first instanceof Map) {
            return processMapValue(columnName, (Map<?, ?>) first, columnInfo);
        } else {
            log.info("check:APP_ID:{} = {} ==== List[0] 非 Map", columnInfo.getAppId(), columnName);
            return first;
        }
    }

    private static void setIfNotDecimal(Map<String, Object> conditions, String column, double defaultValue) {
        String value = Convert.toStr(conditions.get(column));
        if (!NumberUtil.isNumber(value)) {
            conditions.put(column, defaultValue);
            return;
        }
        try {
            double val = Double.parseDouble(value);
            if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                conditions.put(column, defaultValue);
            }
        } catch (NumberFormatException e) {
            conditions.put(column, defaultValue);
        }
    }

    private static void setIfNotNumeric(Map<String, Object> conditions, String column, Object defaultValue) {
        String value = Convert.toStr(conditions.get(column));
        if (!NumberUtil.isNumber(value)) {
            conditions.put(column, defaultValue);
            return;
        }
        try {
            // 检查是否超出 Integer 范围
            long valueLong = Long.parseLong(value);
            if (valueLong > Integer.MAX_VALUE || valueLong < Integer.MIN_VALUE) {
                conditions.put(column, defaultValue);
            }
        } catch (NumberFormatException e) {
            conditions.put(column, defaultValue);
        }
    }

    private static void setIfInvalidDate(Map<String, Object> conditions, String column, String value, String format, Object defaultValue) {
        if (!isValidDateTimeFormat(value, format)) {
            conditions.put(column, defaultValue);
        }
    }

    public static boolean isValidDateTimeFormat(String timeStr, String format) {
        try {
            if (StringUtils.isEmpty(timeStr)) {
                return false;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withResolverStyle(ResolverStyle.STRICT);
            formatter.parse(timeStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
