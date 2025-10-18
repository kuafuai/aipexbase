package com.kuafuai.common.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.google.common.collect.Lists;
import com.kuafuai.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class GeneralTableNameHandler implements TableNameHandler {

    private static final List<String> exculdedTableNameList = Lists.newArrayList(
            "app_info", "app_requirement_sql", "app_sql_execution_log",
            "app_table_column_info", "app_table_info", "app_table_relation",
            "dynamic_api_setting", "delay_task_app_info");

    @Override
    public String dynamicTableName(String sql, String tableName) {
        final String database = DatabaseThreadLocal.getDatabase();
        log.info("GeneralTableNameHandler:{}===========>tableName:{}", database, tableName);

        if (StringUtils.isEmpty(database)) {
            return tableName;
        }

        if (StringUtils.isEmpty(tableName)) {
            return tableName;
        }

        if (exculdedTableNameList.contains(tableName)) {
            return tableName;
        }
        if (tableName.contains(".")) {
            return tableName;
        }

        return "`" + database + "`." + tableName;
    }
}
