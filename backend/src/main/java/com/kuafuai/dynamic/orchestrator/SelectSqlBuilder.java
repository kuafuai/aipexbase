package com.kuafuai.dynamic.orchestrator;

import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.clause.FromClauseBuilder;
import com.kuafuai.dynamic.clause.SelectClauseBuilder;
import com.kuafuai.dynamic.condition.GroupByBuilder;
import com.kuafuai.dynamic.condition.LimitBuilder;
import com.kuafuai.dynamic.condition.OrderByBuilder;
import com.kuafuai.dynamic.condition.WhereBuilder;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.helper.SqlFragments;
import com.kuafuai.dynamic.policy.PolicyEngine;

public class SelectSqlBuilder {
    private final TableContext ctx;

    public SelectSqlBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String buildList() {

        String base = new SelectClauseBuilder(ctx).build();
        String from = new FromClauseBuilder(ctx).build();

        String where = new WhereBuilder(ctx, true).build();

        // 注入 RLS 策略条件
        where = applyRlsPolicy(where, "SELECT");

        String group = new GroupByBuilder(ctx).build();
        String order = new OrderByBuilder(ctx).build();

        StringBuilder sql = new StringBuilder();
        sql.append(SqlFragments.select(base).getText()).append(" ").append(SqlFragments.from(from).getText()).append(" ");

        if (!where.isEmpty()) {
            sql.append(SqlFragments.where(where).getText()).append(" ");
        }
        if (!group.isEmpty()) {
            sql.append(SqlFragments.groupBy(group).getText()).append(" ");
        }
        if (!order.isEmpty()) {
            sql.append(SqlFragments.orderBy(order).getText()).append(" ");
        }

        return sql.toString();
    }

    public String buildOne() {
        String base = buildList();
        String limit = new LimitBuilder(ctx).buildOne();
        return base + " " + SqlFragments.limit(limit).getText();
    }

    public String buildPage() {
        String base = buildList();
        String limit = new LimitBuilder(ctx).build();
        return base + " " + SqlFragments.limit(limit).getText();
    }

    public String buildCount() {
        String where = new WhereBuilder(ctx, false).build();

        // 注入 RLS 策略条件
        where = applyRlsPolicy(where, "SELECT");

        StringBuilder sql = new StringBuilder();
        sql.append(SqlFragments.select("COUNT(*)").getText()).append(" ");
        sql.append(SqlFragments.from(ctx.qualifiedTable()).getText()).append(" ");

        if (!where.isEmpty()) {
            sql.append(SqlFragments.where(where).getText()).append(" ");
        }
        return sql.toString();
    }

    /**
     * 应用 RLS 策略到 WHERE 条件
     *
     * @param where     原有 WHERE 条件
     * @param operation 操作类型
     * @return 注入策略后的 WHERE 条件
     */
    private String applyRlsPolicy(String where, String operation) {
        String policyCondition = PolicyEngine.getUsingCondition(
                ctx.getDatabase(),
                ctx.getTable(),
                operation
        );

        if (StringUtils.isEmpty(policyCondition)) {
            // 没有策略，保持原样
            return where;
        }

        // 有策略，注入到 WHERE 条件
        if (StringUtils.isEmpty(where)) {
            // 原来没有 WHERE 条件，直接使用策略条件
            return policyCondition;
        } else {
            // 原来有 WHERE 条件，用 AND 连接
            return where + " AND (" + policyCondition + ")";
        }
    }
}
