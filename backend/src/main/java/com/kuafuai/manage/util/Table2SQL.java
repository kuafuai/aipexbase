package com.kuafuai.manage.util;


import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.ColumnVo;
import com.kuafuai.manage.entity.vo.TableVo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Table2SQL {


    public static String table2SQL(String appId, TableVo tableVo) {
        List<String> columnDefs = new ArrayList<>();
        String primaryKey = null;

        for (ColumnVo col : tableVo.getColumns()) {
            if (col.getIsPrimary()) {
                primaryKey = col.getColumnName();
            }
            columnDefs.add(generateColumnDefinition(col));
        }

        if (primaryKey != null) {
            columnDefs.add("PRIMARY KEY (`" + primaryKey + "`)");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `")
                .append(appId).append("`.`")
                .append(tableVo.getTableName()).append("` (\n")
                .append(StringUtils.join(columnDefs, ",\n"))
                .append("\n)");

        if (StringUtils.isNotEmpty(tableVo.getDescription())) {
            sb.append(" COMMENT='").append(tableVo.getDescription()).append("'");
        }

        sb.append(";\n");
        return sb.toString();

    }

    public static String generateColumnDefinition(ColumnVo column) {
        String type = resolveColumnType(column);
        StringBuilder col = new StringBuilder();
        col.append("`").append(column.getColumnName()).append("` ").append(type);

        if (column.getIsNullable()) {
            if (StringUtils.equalsIgnoreCase(column.getDslType(), "datetime")) {
                col.append(" DEFAULT CURRENT_TIMESTAMP ");
            } else if (StringUtils.equalsIgnoreCase(column.getDslType(), "date")) {
                col.append(" DEFAULT (CURRENT_DATE) ");
            } else {
                col.append(" NOT NULL ");
            }
        } else {
            if (StringUtils.equalsIgnoreCase(column.getDslType(), "datetime")) {
                col.append(" DEFAULT CURRENT_TIMESTAMP ");
            } else if (StringUtils.equalsIgnoreCase(column.getDslType(), "date")) {
                col.append(" DEFAULT (CURRENT_DATE) ");
            }
        }

        if (column.getIsPrimary()) {
            col.append(" AUTO_INCREMENT ");
        }

        if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
            String safeComment = column.getColumnComment().replace("'", "");
            col.append(" COMMENT '").append(safeComment).append("'");
        }

        return col.toString();
    }

    public static String resolveColumnType(ColumnVo column) {
        String type = column.getColumnType();
        if (column.getIsPrimary()) return "INT";

        String dsl = column.getDslType() != null ? column.getDslType().toLowerCase() : "";

        if (Arrays.asList("image", "images", "file", "files", "video", "videos").contains(dsl)) {
            type = "VARCHAR(512)";
        } else if (Arrays.asList("audio", "audios", "ai").contains(dsl)) {
            type = "VARCHAR(512)";
        } else if ("array".equalsIgnoreCase(type)) {
            if (StringUtils.isNotEmpty(column.getReferenceTableName())) {
                type = "INT";
            } else {
                type = "VARCHAR(255)";
            }
        }
        return type;
    }

    public static String resolveDslType(ColumnVo column) {
        if (column.getIsPrimary()) return "int";

        String type = column.getColumnType() != null ? column.getColumnType().toLowerCase() : "";
        if ("array".equals(type) && StringUtils.isNotEmpty(column.getReferenceTableName())) {
            return "quote";
        }

        String dsl = column.getDslType() != null ? column.getDslType().toLowerCase() : "";
        if ("audio".equals(dsl) || "audios".equals(dsl)) {
            return "file";
        }
        if (StringUtils.isEmpty(dsl)) {
            return column.getColumnType().toLowerCase();
        }
        return column.getDslType();
    }


}
