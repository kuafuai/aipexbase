package com.kuafuai.manage.mcp.tool;

import com.google.gson.reflect.TypeToken;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.mcp.server.McpSyncServerExchange;
import com.kuafuai.manage.mcp.service.McpBusinessService;
import com.kuafuai.manage.mcp.spec.McpSchema;
import com.kuafuai.manage.mcp.util.SchemaBuilder;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.system.service.ApplicationAPIKeysService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ExecuteSQLToolProvider implements MCPToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteSQLToolProvider.class);

    @Resource
    private ApplicationAPIKeysService applicationAPIKeysService;

    @Resource
    private ManageBusinessService manageBusinessService;

    @Resource
    private McpBusinessService mcpBusinessService;


    @Override
    public McpSchema.Tool getToolDefinition() {
        String toolName = "execute_sql";
        String description = "当你设计好了 mysql 表结构对应的的 json 字符串后，使用此工具并将字符串传入 `ddl_query` 变量然后调用去创建表结构";
        Map<String, Object> inputSchemaMap = SchemaBuilder.create()
                .addProperty("ddl_query", "string", "一段固定格式的 json 数据，用于表示一组表结构元数据")
                .build();

        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema("object", inputSchemaMap, Collections.singletonList("ddl_query"), null);

        return McpSchema.Tool.builder()
                .name(toolName)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }

    @Override
    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, Map<String, Object> params) {
        List<McpSchema.Content> result = new ArrayList<>();
        try {
            String appId = mcpBusinessService.getAppIdByToken(exchange);
            if (StringUtils.isBlank(appId)) {
                // 返回无权访问
                McpSchema.Content content = new McpSchema.TextContent("TOKEN 无权");
                result.add(content);
                return McpSchema.CallToolResult.builder()
                        .content(result)
                        .isError(true)
                        .build();
            }
            String ddlQuery = (String) params.get("ddl_query");
            System.out.println(ddlQuery);


            List<TableVo> tableVos = JSON.parseObject(ddlQuery, new TypeToken<List<TableVo>>() {
            }.getType());

            if (manageBusinessService.createTables(appId,tableVos)) {
                McpSchema.Content content = new McpSchema.TextContent("本次表结构创建完毕: "+ddlQuery);
                result.add(content);
                return McpSchema.CallToolResult.builder()
                        .content(result)
                        .isError(false)
                        .build();
            } else {
                logger.info("未知异常: {}",ddlQuery);
                // 未知异常
                McpSchema.Content content = new McpSchema.TextContent("未知异常:" + ddlQuery);
                result.add(content);
                return McpSchema.CallToolResult.builder()
                        .content(result)
                        .isError(true)
                        .build();
            }

        } catch (Exception e) {
            logger.error("execute_sql: invoke failed {}", e.getMessage());
            McpSchema.Content content = new McpSchema.TextContent("工具执行异常: " + e.getMessage());
            result.add(content);
            return McpSchema.CallToolResult.builder()
                    .content(result)
                    .isError(true)
                    .build();
        }
    }
}
