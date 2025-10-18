package com.kuafuai.manage.mcp.tool;

import com.kuafuai.common.util.StringUtils;
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
public class ListDynamicApiToolProvider implements MCPToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(ListDynamicApiToolProvider.class);

    @Resource
    private ApplicationAPIKeysService applicationAPIKeysService;

    @Resource
    private ManageBusinessService manageBusinessService;

    @Resource
    private McpBusinessService mcpBusinessService;


    @Override
    public McpSchema.Tool getToolDefinition() {
        String toolName = "list_dynamic_api";
        String description = "当你需要使用外部服务如查询天气、发送飞书消息、调用LLM制作聊天应用时，务必使用此工具获取当前应用下支持的所有第三方服务提供商提供的能力，工具会返回 结合 baas_js sdk 的 API 调用方式，可以根据调用方式构建你的应用";

        Map<String, Object> inputSchemaMap = SchemaBuilder.create().build();
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema("object", inputSchemaMap,null, null);

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
            McpSchema.Content content = new McpSchema.TextContent("这里有一些第三方服务的 API，你可以参照这些 api 的调用方式结合 baas_js 制作你的应用:\n"+mcpBusinessService.getBaasUseApiDescription(appId));
            result.add(content);
            return McpSchema.CallToolResult.builder()
                    .content(result)
                    .isError(false)
                    .build();

        } catch (Exception e) {
            logger.error("list_dynamic_api: invoke failed {}", e.getMessage());
            McpSchema.Content content = new McpSchema.TextContent("list_dynamic_api 工具执行异常: " + e.getMessage());
            result.add(content);
            return McpSchema.CallToolResult.builder()
                    .content(result)
                    .isError(true)
                    .build();
        }
    }
}
