package com.kuafuai.manage.mcp.tool;

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
import java.util.List;
import java.util.Map;

@Component
public class ListTablesToolProvider implements MCPToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(ListTablesToolProvider.class);

    @Resource
    private ApplicationAPIKeysService applicationAPIKeysService;

    @Resource
    private ManageBusinessService manageBusinessService;

    @Resource
    private McpBusinessService mcpBusinessService;

    @Override
    public McpSchema.Tool getToolDefinition() {

        String toolName = "list_tables";
        String description = "此工具可以查询应用下所有表结构数据，当你需要获取表结构数据以更好的支撑你的业务场景代码，你可以频繁的调用此工具获取表结构数据，【注意】此工具并不会输出表内的业务数据";
        Map<String, Object> inputSchemaMap = SchemaBuilder.create().build();

        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema("object", inputSchemaMap, null, null);
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
                McpSchema.Content content = new McpSchema.TextContent("无权访问");
                result.add(content);
                return McpSchema.CallToolResult.builder()
                        .content(result)
                        .isError(true)
                        .build();
            }
            List<TableVo> databaseSchemaSet = manageBusinessService.getTablesByAppId(appId);
            if (databaseSchemaSet!=null) {
                String jsonString = JSON.toJSONString(databaseSchemaSet);
                McpSchema.Content content = new McpSchema.TextContent(jsonString);
                result.add(content);
            } else {
                // 未知异常
                McpSchema.Content content = new McpSchema.TextContent("当前应用下无表结构");
                result.add(content);
            }
            return McpSchema.CallToolResult.builder()
                    .content(result)
                    .isError(false)
                    .build();

        } catch (Exception e) {
            logger.error("invoke failed: {}", e.getMessage());
            McpSchema.Content content = new McpSchema.TextContent("工具执行异常: " + e.getMessage());
            result.add(content);
            return McpSchema.CallToolResult.builder()
                    .content(result)
                    .isError(true)
                    .build();
        }
    }
}
