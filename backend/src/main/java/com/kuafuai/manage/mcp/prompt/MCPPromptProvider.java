package com.kuafuai.manage.mcp.prompt;

import com.kuafuai.manage.mcp.server.McpSyncServerExchange;
import com.kuafuai.manage.mcp.spec.McpSchema;

public interface MCPPromptProvider {
    McpSchema.Prompt getPromptDefinition();

    McpSchema.GetPromptResult getPrompt(McpSyncServerExchange exchange, McpSchema.GetPromptRequest getPromptRequest);
}
