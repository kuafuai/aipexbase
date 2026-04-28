package com.kuafuai.dynamic.validation;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.AppTableColumnInfo;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;

/**
 * 查询条件验证器
 */
@Slf4j
public class QueryConditionValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    /**
     * 验证查询条件中的日期值
     */
    public static void validate(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        for (AppTableColumnInfo column : columns) {
            String dslType = column.getDslType();
            // 处理 date 和 datetime 类型
            if (!"date".equalsIgnoreCase(dslType) && !"datetime".equalsIgnoreCase(dslType)) {
                continue;
            }

            String columnName = column.getColumnName();
            Object value = conditions.get(columnName);

            if (value != null) {
                validateDateValue(columnName, value, dslType);
            }
        }
    }

    /**
     * 递归验证日期值
     */
    private static void validateDateValue(String columnName, Object value, String dslType) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            for (Object v : map.values()) {
                validateDateValue(columnName, v, dslType);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object item : list) {
                validateDateValue(columnName, item, dslType);
            }
        } else {
            if (value != null) {
                validateDateString(columnName, Convert.toStr(value), dslType);
            }
        }
    }

    /**
     * 验证日期字符串
     */
    private static void validateDateString(String columnName, String dateStr, String dslType) {
        if (StringUtils.isEmpty(dateStr)) {
            return;
        }

        try {
            if ("datetime".equalsIgnoreCase(dslType)) {
                // datetime 类型：支持 "2026-04-31" 或 "2026-04-31 00:00:00"
                if (dateStr.contains(" ")) {
                    // 包含时间部分
                    LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
                } else {
                    // 只有日期部分
                    LocalDate.parse(dateStr, DATE_FORMATTER);
                }
            } else {
                // date 类型：只支持 "2026-04-31"
                LocalDate.parse(dateStr, DATE_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException("字段 " + columnName + " 的日期参数不正确: " + dateStr);
        }
    }
}
