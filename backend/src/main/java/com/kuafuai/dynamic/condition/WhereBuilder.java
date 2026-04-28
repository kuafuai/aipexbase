package com.kuafuai.dynamic.condition;

import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.helper.DynamicWhereStatement;
import com.kuafuai.dynamic.validation.QueryConditionValidator;

import java.util.List;

public class WhereBuilder {

    private final TableContext ctx;
    private final boolean useAlias;

    public WhereBuilder(TableContext ctx, boolean useAlias) {
        this.ctx = ctx;
        this.useAlias = useAlias;
    }

    public String build() {
        // 验证查询条件中的日期值
        QueryConditionValidator.validate(ctx.getColumns(), ctx.getConditions());

        List<String> parts = DynamicWhereStatement.gen_where_statement(
                useAlias ? ctx.tableAlias() : null, ctx.getColumns(), ctx.getConditions(), useAlias
        );
        return parts.isEmpty() ? "" : String.join(" AND ", parts);
    }
}
