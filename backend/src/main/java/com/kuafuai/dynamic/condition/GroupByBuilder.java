package com.kuafuai.dynamic.condition;

import com.google.common.collect.Lists;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.kuafuai.dynamic.helper.DynamicSelectStatement.getResourceColumns;

public class GroupByBuilder {
    private final TableContext ctx;

    public GroupByBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        List<String> groups = Lists.newArrayList();

        Map<String, Object> conditions = ctx.getConditions();

        List<AppTableColumnInfo> resourceCols = getResourceColumns(this.ctx.getColumns());
        String database = this.ctx.getDatabase();
        String table = this.ctx.getTable();

        if (!resourceCols.isEmpty() && resourceCols.size() <= 2) {
            SystemBusinessService service = SpringUtils.getBean(SystemBusinessService.class);
            String pk = service.getAppTablePrimaryKey(database, table);
            String resourceGroupBy = String.format("`%s`.%s", table, pk);
            groups.add(resourceGroupBy);
        }

        Object group = conditions.get("group_by");
        if (Objects.nonNull(group)) {
            groups.add(String.valueOf(group));
        }
        return groups.isEmpty() ? "" : String.join(" , ", groups);
    }
}
