package com.kuafuai.dynamic.orchestrator;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.clause.DeleteClauseBuilder;
import com.kuafuai.dynamic.condition.WhereBuilder;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.policy.PolicyEngine;

public class DeleteSqlBuilder {
    private final TableContext ctx;

    public DeleteSqlBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        String where = new WhereBuilder(ctx, false).build();

        // 注入 RLS 策略条件
        String policyCondition = PolicyEngine.getUsingCondition(
                ctx.getDatabase(),
                ctx.getTable(),
                "DELETE"
        );

        if (StringUtils.isNotEmpty(policyCondition)) {
            if (StringUtils.isEmpty(where)) {
                where = policyCondition;
            } else {
                where = where + " AND (" + policyCondition + ")";
            }
        }

        if (where.isEmpty()) {
            throw new BusinessException("dynamic.delete.where");
        }

        return new DeleteClauseBuilder(ctx).buildBase() + " WHERE " + where;
    }
}
