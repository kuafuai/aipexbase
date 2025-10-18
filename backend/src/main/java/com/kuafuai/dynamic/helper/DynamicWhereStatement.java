package com.kuafuai.dynamic.helper;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicWhereStatement {


    private static final Map<String, String> OPERATOR_MAP = ImmutableMap.<String, String>builder()
            .put("eq", "=")
            .put("neq", "!=")
            .put("gte", ">=")
            .put("$gte", ">=")
            .put("lte", "<=")
            .put("$lte", "<=")
            .put("gt", ">")
            .put("$gt", ">")
            .put("lt", "<")
            .put("$lt", "<")
            .build();

    private static final Set<String> EXACT_MATCH_TYPES = Sets.newHashSet("keyword", "int", "number", "boolean", "quote");


    /**
     * where
     *
     * @param table      table
     * @param columns    columns
     * @param conditions cond
     * @return list
     */
    public static List<String> gen_where_statement(String table, List<AppTableColumnInfo> columns, Map<String, Object> conditions,
                                                   boolean useAlias) {
        Map<String, AppTableColumnInfo> columnMaps = columns.stream()
                .collect(Collectors.toMap(AppTableColumnInfo::getColumnName, Function.identity()));

        List<String> query = Lists.newArrayList();

        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();

            // 顶层判断or
            if ("or".equalsIgnoreCase(key) && entry.getValue() instanceof List) {
                handleGlobalOrCondition(query, table, (List<?>) entry.getValue(), columnMaps, useAlias);
                continue;
            }

            if (!columnMaps.containsKey(key)) {
                continue;
            }

            Object objValue = entry.getValue();
            if (objValue instanceof Map) {
                handleMapCondition(query, table, key, (Map<String, Object>) objValue, useAlias, "");
            } else if (objValue instanceof List) {
                // 默认认为是 in 查询
                handleInCondition(query, table, key, (List<Object>) objValue, useAlias, false, "");
            } else {
                handleSingleCondition(query, table, key, objValue, columnMaps.get(key), useAlias, "");
            }
        }

        return query;
    }

    private static void handleGlobalOrCondition(List<String> query, String table, List<?> orConditions, Map<String, AppTableColumnInfo> columnMaps, boolean useAlias) {
        List<String> andClauses = new ArrayList<>();

        for (int i = 0; i < orConditions.size(); i++) {
            Object cond = orConditions.get(i);
            if (!(cond instanceof Map)) continue;

            Map<String, Object> condMap = (Map<String, Object>) cond;

            List<String> innerOr = new ArrayList<>();

            for (Map.Entry<String, Object> entry : condMap.entrySet()) {
                String key = entry.getKey();
                if (!columnMaps.containsKey(key)) continue;

                Object value = entry.getValue();

                List<String> subQuery = new ArrayList<>();

                if (value instanceof Map) {
                    handleMapCondition(subQuery, table, key, (Map<String, Object>) value, useAlias, "or[" + i + "].");
                } else if (value instanceof List) {
                    handleInCondition(subQuery, table, key, (List<?>) value, useAlias, false, "or[" + i + "].");
                } else {
                    handleSingleCondition(subQuery, table, key, value, columnMaps.get(key), useAlias, "or[" + i + "].");
                }

                if (!subQuery.isEmpty()) {
                    innerOr.addAll(subQuery);
                }

            }

            if (!innerOr.isEmpty()) {
                andClauses.add(" ( " + String.join(" OR ", innerOr) + " ) ");
            }
        }

        if (!andClauses.isEmpty()) {
            query.add(" ( " + String.join(" AND ", andClauses) + " ) ");
        }
    }

    private static String prefixColumn(String tableOrAlias, String column, boolean useAlias) {
        return useAlias && StringUtils.isNotEmpty(tableOrAlias)
                ? "`" + tableOrAlias + "`." + column
                : "`" + column + "`";
    }

    private static void handleMapCondition(List<String> query, String table, String key, Map<String, Object> objMapValue, boolean useAlias, String prefix) {
        if (objMapValue.isEmpty()) return;
        for (Map.Entry<String, Object> mapEntry : objMapValue.entrySet()) {
            String opKey = mapEntry.getKey().toLowerCase();

            if ("between".equals(opKey) && mapEntry.getValue() instanceof List) {
                List<?> betweenVals = (List<?>) mapEntry.getValue();
                if (betweenVals.size() == 2) {
                    query.add(prefixColumn(table, key, useAlias) + " BETWEEN #{conditions." + prefix + key + ".between[0]} AND #{conditions." + prefix + key + ".between[1]}");
                }
                continue;
            }

            if ("or".equals(opKey) && mapEntry.getValue() instanceof List) {
                handleOrCondition(query, table, key, (List<?>) mapEntry.getValue(), useAlias, prefix);
                continue;
            }

            if ("in".equals(opKey) && mapEntry.getValue() instanceof List) {
                handleInCondition(query, table, key, (List<?>) mapEntry.getValue(), useAlias, true, prefix);
                continue;
            }

            String operator = OPERATOR_MAP.get(opKey);
            if (operator != null) {
                query.add(prefixColumn(table, key, useAlias) + " " + operator + " #{conditions." + prefix + key + "." + mapEntry.getKey() + "}");
            }
        }
    }

    private static void handleInCondition(List<String> query, String table, String key, List<?> values, boolean useAlias, boolean useIn, String prefix) {
        if (values.isEmpty())
            return;

        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Object val = values.get(i);
            // 跳过 Map 类型
            if (val instanceof Map) {
                continue;
            }
            String strVal = Convert.toStr(val);
            if (StringUtils.isNotEmpty(strVal)) {
                if (useIn) {
                    placeholders.add("#{conditions." + prefix + key + ".in[" + i + "]}");
                } else {
                    placeholders.add("#{conditions." + prefix + key + "[" + i + "]}");
                }
            }
        }

        if (!placeholders.isEmpty()) {
            query.add(prefixColumn(table, key, useAlias) + " IN (" + String.join(", ", placeholders) + ")");
        }
    }


    private static void handleOrCondition(List<String> query, String table, String key, List<?> values, boolean useAlias, String prefix) {
        if (values.isEmpty())
            return;
        List<String> orClauses = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            String val = Convert.toStr(values.get(i));
            if (StringUtils.isNotEmpty(val)) {
                orClauses.add(prefixColumn(table, key, useAlias) + " = #{conditions." + prefix + key + ".or[" + i + "]}");
            }
        }
        if (!orClauses.isEmpty()) {
            query.add("( " + String.join(" OR ", orClauses) + " )");
        }
    }

    private static void handleSingleCondition(List<String> query, String table, String key, Object value, AppTableColumnInfo columnInfo, boolean useAlias, String prefix) {
        String strValue = Convert.toStr(value);
        if (StringUtils.isNotEmpty(strValue)) {
            if (EXACT_MATCH_TYPES.contains(columnInfo.getDslType().toLowerCase())) {
                query.add(prefixColumn(table, key, useAlias) + " = #{conditions." + prefix + key + "}");
            } else {
                query.add(prefixColumn(table, key, useAlias) + " LIKE CONCAT('%', #{conditions." + prefix + key + "}, '%')");
            }
        }
    }

}
