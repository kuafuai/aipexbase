package com.kuafuai.manage.mcp.tool;

import com.kuafuai.manage.mcp.server.McpSyncServerExchange;
import com.kuafuai.manage.mcp.spec.McpSchema;

import java.util.Map;

public interface MCPToolProvider {
    McpSchema.Tool getToolDefinition();

    McpSchema.CallToolResult execute(McpSyncServerExchange exchange, Map<String, Object> params);
}
