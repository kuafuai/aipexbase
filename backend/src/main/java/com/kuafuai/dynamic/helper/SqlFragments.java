package com.kuafuai.dynamic.helper;

import com.kuafuai.dynamic.context.SqlPart;

public class SqlFragments {

    public static SqlPart select(String selectClause) {
        return SqlPart.of("SELECT " + selectClause);
    }

    public static SqlPart from(String fromClause) {
        return SqlPart.of("FROM " + fromClause);
    }

    public static SqlPart where(String whereClause) {
        if (whereClause == null || whereClause.trim().isEmpty())
            return SqlPart.of("");
        return SqlPart.of("WHERE " + whereClause);
    }

    public static SqlPart groupBy(String groupByClause) {
        if (groupByClause == null || groupByClause.trim().isEmpty())
            return SqlPart.of("");
        return SqlPart.of("GROUP BY " + groupByClause);
    }

    public static SqlPart orderBy(String orderByClause) {
        if (orderByClause == null || orderByClause.trim().isEmpty())
            return SqlPart.of("");
        return SqlPart.of("ORDER BY " + orderByClause);
    }

    public static SqlPart limit(String limitClause) {
        if (limitClause == null || limitClause.trim().isEmpty())
            return SqlPart.of("");
        return SqlPart.of("LIMIT " + limitClause);
    }
}
