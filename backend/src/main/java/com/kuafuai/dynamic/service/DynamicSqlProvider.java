package com.kuafuai.dynamic.service;

import com.google.common.collect.Lists;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.entity.AppTableInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DynamicSqlProvider {


    public static String buildDynamicInsertBatch(String database,
                                                 String table,
                                                 List<Map<String, Object>> conditions) {
        if (ObjectUtils.isEmpty(conditions)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "insert data is null");
        }

        // 检测database和table
        check_database_and_table(database, table);

        // 查询出 表字段
        List<AppTableColumnInfo> columns = get_table_columns(database, table);

        // 检测必填字段
        for (Map<String, Object> condition : conditions) {
            check_table_column_nullable(columns, condition);
        }

        Map<String, AppTableColumnInfo> columnMaps = columns.stream()
                .filter(column -> !column.isPrimary())
                .collect(Collectors.toMap(AppTableColumnInfo::getColumnName, Function.identity()));

        final Map<String, Object> objectMap = conditions.get(0);

        List<String> keysSqlList = Lists.newArrayList();
        List<String> keysList = Lists.newArrayList();
        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
            String key = entry.getKey();
            if (columnMaps.containsKey(key)) {
                keysSqlList.add("`" + key + "`");
                keysList.add(key);
            }
        }

        List<String> valueSqlList = Lists.newArrayList();

        for (int i = 0; i < conditions.size(); i++) {
            Map<String, Object> row = conditions.get(i);
            List<String> valueItem = new ArrayList<>();
            for (String col : keysList) {
                // MyBatis 参数占位符，需要带索引
                valueItem.add("#{" + "conditions[" + i + "]." + col + "}");
            }
            String valueClause = "(" + String.join(", ", valueItem) + ")";
            valueSqlList.add(valueClause);
        }


        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append("`" + database + "`").append(".").append(table)
                .append(" (")
                .append(String.join(", ", keysSqlList))
                .append(") VALUES ");

        sql.append(String.join(", ", valueSqlList));

        log.info("===========insert batch sql : {}", sql);
        return sql.toString();
    }

    public static String buildDynamicDeleteBatch(String database,
                                                 String table,
                                                 Map<String, Object> conditions) {
        if (ObjectUtils.isEmpty(conditions)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "delete data is null");
        }

        // 检测 database 和 table
        check_database_and_table(database, table);

        // 查询出表字段
        List<AppTableColumnInfo> columns = get_table_columns(database, table);
        AppTableColumnInfo primaryColumn = columns.stream()
                .filter(AppTableColumnInfo::isPrimary)
                .findFirst()
                .get();        // 确认主键字段存在
        if (ObjectUtils.isEmpty(primaryColumn)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主键字段不存在或未设置为主键");
        }
        String primaryKey = primaryColumn.getColumnName();
        // 取出 ids
        @SuppressWarnings("unchecked")
        List<Object> ids = (List<Object>) conditions.get("ids");

        // 构建占位符
        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            placeholders.add("#{conditions.ids[" + i + "]}");
        }

        // 拼接SQL
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ")
                .append("`").append(database).append("`").append(".").append(table)
                .append(" WHERE `").append(primaryKey).append("` IN (")
                .append(String.join(", ", placeholders))
                .append(")");

        log.info("===========delete batch sql : {}", sql);
        return sql.toString();
    }


    /**
     * 检测 表是否存在
     *
     * @param database
     * @param table
     */
    private static void check_database_and_table(String database, String table) {

        DynamicInfoCache dynamicInfoCache = SpringUtils.getBean(DynamicInfoCache.class);
        AppTableInfo tableInfo = dynamicInfoCache.getAppTableInfo(database, table);
        if (tableInfo == null) {
            throw new BusinessException(database + ":" + table + ":数据不存在，请检查=====>表不存在");
        }
    }

    /**
     * 查询表所有字段
     *
     * @param database
     * @param table
     * @return
     */

    private static List<AppTableColumnInfo> get_table_columns(String database, String table) {
        DynamicInfoCache dynamicInfoCache = SpringUtils.getBean(DynamicInfoCache.class);

        return dynamicInfoCache.getAppTableColumnInfo(database, table);
    }

    /**
     * 检测表字段必填项目
     *
     * @param columns
     * @param conditions
     */
    private static void check_table_column_nullable(List<AppTableColumnInfo> columns, Map<String, Object> conditions) {
        List<AppTableColumnInfo> list = columns.stream()
                .filter(column -> !column.isNullable() && !column.isPrimary())
                .collect(Collectors.toList());

        for (AppTableColumnInfo columnInfo : list) {
            String columnName = columnInfo.getColumnName();
            if (!conditions.containsKey(columnName) || Objects.isNull(conditions.get(columnName))) {
                throw new BusinessException(columnInfo.getAppId() + ":字段:" + columnName + "的值不能为空");
            }
        }
    }
}
