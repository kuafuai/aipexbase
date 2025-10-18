package com.kuafuai.manage.util;

import com.google.common.collect.Lists;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.ColumnVo;
import com.kuafuai.manage.entity.vo.TableVo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SystemTableUtil {

    public final static String[] login_columns = new String[]{
            "relevance_id",
            "relevance_table",
            "wx_open_id",
            "phone_number",
            "user_name",
            "password",
    };

    public final static String[] static_resources_column = new String[]{
            "resource_type",
            "resource_name",
            "resource_path",
            "related_table_name",
            "relate_table_column_name",
            "related_table_key",
    };


    public final static String[] kf_system_config = new String[]{
            "name",
            "chinese_name",
            "description",
            "content",
            "remark",
            "type"
    };

    public static TableVo gen_static_table(String appId, String tableName, String comment,
                                           String primaryKey, String[] strColumns) {
        TableVo tableVo = TableVo.builder()
                .appId(appId)
                .tableName(tableName)
                .description(comment)
                .build();

        List<ColumnVo> columns = Lists.newArrayList();
        // 主键
        columns.add(buildColumn(primaryKey, "int", "主键", true, true, "int"));

        for (String columnName : strColumns) {
            String dslType = StringUtils.equalsIgnoreCase(columnName, "password") ? "password" : "keyword";
            columns.add(buildColumn(columnName, dslType, columnName, false, false, dslType));
        }

        tableVo.setColumns(columns);
        return tableVo;
    }

    private static ColumnVo buildColumn(String name, String type, String comment,
                                        boolean primaryKey,
                                        boolean nullable, String dslType) {

        return ColumnVo.builder()
                .columnName(name)
                .columnType(type)
                .dslType(dslType)
                .columnComment(comment)
                .isPrimary(primaryKey)
                .isNullable(nullable)
                .isShow(false)
                .build();
    }

}
