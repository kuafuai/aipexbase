package com.kuafuai.dynamic.condition;

import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kuafuai.dynamic.helper.DynamicSelectStatement.getResourceColumns;

public class OrderByBuilder {
    private final TableContext ctx;
    private static final String DEFAULT_DIRECTION = "ASC";


    public OrderByBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        Map<String, Object> conditions = ctx.getConditions();
        Object order = conditions.get("order_by");
        if (order == null) {
            List<AppTableColumnInfo> resourceCols = getResourceColumns(this.ctx.getColumns());
            if (!resourceCols.isEmpty() && resourceCols.size() <= 2) {
                String database = this.ctx.getDatabase();
                String table = this.ctx.getTable();

                SystemBusinessService service = SpringUtils.getBean(SystemBusinessService.class);
                String pk = service.getAppTablePrimaryKey(database, table);
                return String.format("`%s`.%s", table, pk) + " " + DEFAULT_DIRECTION;
            }
            return "";
        }

        String orderClause;

        if (order instanceof Map) {
            // Map: {checkin_date=desc, id=asc}
            Map<?, ?> orderMap = (Map<?, ?>) order;

            orderClause = orderMap.entrySet().stream()
                    .map(entry -> buildOrderItem(String.valueOf(entry.getKey()), entry.getValue()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));

        } else if (order instanceof List) {
            // List<Map>: [{field=xxx, direction=desc}, {field=yyy, direction=asc}]
            List<?> orderList = (List<?>) order;

            orderClause = orderList.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<?, ?> map = (Map<?, ?>) item;
                        return buildOrderItem(String.valueOf(map.get("field")), map.get("direction"));
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        } else {
            // String: "checkin_date desc"
            orderClause = String.valueOf(order);
        }
        return orderClause;
    }


    /**
     * 构建单个排序片段
     */
    private String buildOrderItem(String field, Object directionObj) {
        List<AppTableColumnInfo> columns = ctx.getColumns();

        boolean validField = columns.stream().anyMatch(c -> field.equalsIgnoreCase(c.getColumnName()));
        if (!validField) {
            return null;
        }

        String direction = directionObj == null ? DEFAULT_DIRECTION : String.valueOf(directionObj).toUpperCase();
        direction = validateDirection(direction);
        return "`" + field + "` " + direction;
    }


    /**
     * 校验排序方向
     */
    private String validateDirection(String direction) {
        return ("ASC".equals(direction) || "DESC".equals(direction)) ? direction : DEFAULT_DIRECTION;
    }
}
