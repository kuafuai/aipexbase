package com.kuafuai.dynamic.helper;

import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppTableColumnInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DynamicSelectStatement {

    private static final String[] RESOURCE_TYPES = {"image", "images", "file", "files", "video", "videos"};


    public static String gen_select_statement(String database, String table, Map<String, Object> conditions) {

        SystemBusinessService service = SpringUtils.getBean(SystemBusinessService.class);
        String pk = service.getAppTablePrimaryKey(database, table);
        List<AppTableColumnInfo> columns = get_table_columns(database, table);

        boolean showPassword = conditions.containsKey("system_show_password");
        String baseSelectCols = buildSelectColumns(table, columns, showPassword);

        // 处理资源列
        List<AppTableColumnInfo> resourceCols = getResourceColumns(columns);
        StringBuilder selectBuilder = new StringBuilder(baseSelectCols);

        String fromClause = buildFromClause(database, table, pk, resourceCols, selectBuilder);

        // group by 如果有资源 join
        String groupByColumn = (!resourceCols.isEmpty() && resourceCols.size() <= 2) ? String.format("`%s`.%s", table, pk) : null;

        // where 条件
        List<String> whereConditions = DynamicWhereStatement.gen_where_statement(table, columns, conditions, true);

        String sql = buildSQL(selectBuilder.toString(), fromClause, whereConditions, groupByColumn);

        log.info("dynamicSQLBuilder=========================={}", sql);
        return sql;
    }

    private static String buildSQL(String selectCols, String fromClause, List<String> whereConditions,
                                   String groupByColumn) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(selectCols);

        sql.append(" FROM ").append(fromClause);

        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
        }

        if (StringUtils.isNotBlank(groupByColumn)) {
            sql.append(" GROUP BY ").append(groupByColumn);
        }

        return sql.toString();
    }

    private static String buildSelectColumns(String table, List<AppTableColumnInfo> columns, boolean showPassword) {
        return columns.stream()
                .filter(c -> !isResource(c) && (showPassword || !isPassword(c)))
                .map(c -> String.format("`%s`.%s", table, c.getColumnName()))
                .collect(Collectors.joining(", "));
    }

    private static String buildFromClause(String database, String table, String pk,
                                          List<AppTableColumnInfo> resourceCols, StringBuilder selectBuilder) {
        if (!resourceCols.isEmpty() && resourceCols.size() <= 2) {
            return buildResourceJoinAndSelect(database, table, pk, resourceCols, selectBuilder);
        } else {
            return String.format(" `%s`.`%s` `%s` ", database, table, table);
        }
    }

    private static String buildResourceJoinAndSelect(String database, String table, String pk,
                                                     List<AppTableColumnInfo> resourceCols, StringBuilder selectBuilder) {
        StringBuilder fromBuilder = new StringBuilder(String.format(" `%s`.`%s` `%s` ", database, table, table));

        for (AppTableColumnInfo col : resourceCols) {
            String alias = col.getColumnName();

            String joinSubquery = String.format(
                    "(SELECT * FROM `%s`.static_resources " +
                            "WHERE related_table_name='%s' " +
                            "AND relate_table_column_name='%s') AS %s",
                    database, table, alias, alias
            );

            fromBuilder.append("\n LEFT JOIN ")
                    .append(joinSubquery)
                    .append(" ON ")
                    .append(alias).append(".related_table_key = ")
                    .append("`").append(table).append("`.").append(pk).append(" ");

            // 添加 JSON 聚合列
            selectBuilder.append(", JSON_ARRAYAGG(JSON_OBJECT( ")
                    .append("'resource_id', ").append(alias).append(".resource_id, ")
                    .append("'resource_path', ").append(alias).append(".resource_path, ")
                    .append("'url', ").append(alias).append(".resource_path, ")
                    .append("'related_table_name', ").append(alias).append(".related_table_name, ")
                    .append("'relate_table_column_name', ").append(alias).append(".relate_table_column_name, ")
                    .append("'related_table_key', ").append(alias).append(".related_table_key")
                    .append(")) AS ").append(alias);
        }
        return fromBuilder.toString();
    }


    public static List<AppTableColumnInfo> getResourceColumns(List<AppTableColumnInfo> columns) {
        return columns.stream()
                .filter(DynamicSelectStatement::isResource)
                .collect(Collectors.toList());
    }


    public static boolean isResource(AppTableColumnInfo col) {
        return StringUtils.equalsAnyIgnoreCase(col.getDslType(), RESOURCE_TYPES);
    }

    public static boolean isPassword(AppTableColumnInfo col) {
        return "password".equalsIgnoreCase(col.getDslType());
    }


    private static List<AppTableColumnInfo> get_table_columns(String database, String table) {
        DynamicInfoCache dynamicInfoCache = SpringUtils.getBean(DynamicInfoCache.class);

        return dynamicInfoCache.getAppTableColumnInfo(database, table);
    }
}
