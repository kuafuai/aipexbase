package com.kuafuai.manage.mcp.service;

import com.kuafuai.manage.mcp.prompt.MCPPromptProvider;
import com.kuafuai.manage.mcp.server.McpServerFeatures;
import com.kuafuai.manage.mcp.server.McpSyncServer;
import com.kuafuai.manage.mcp.server.McpSyncServerExchange;
import com.kuafuai.manage.mcp.spec.McpSchema;
import com.kuafuai.manage.mcp.tool.MCPToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class MCPService {

    private static final Logger logger = LoggerFactory.getLogger(MCPService.class);

    @Resource
    private McpSyncServer mcpServer;

    @Resource
    private ApplicationContext applicationContext;


    /**
     * 初始化MCP工具
     */
    @PostConstruct
    public void initializeMCPSystem() {
        logger.info("正在初始化内置 MCP 工具...");
        Map<String, MCPToolProvider> tools = applicationContext.getBeansOfType(MCPToolProvider.class);
        for (Map.Entry<String, MCPToolProvider> entry : tools.entrySet()) {
            MCPToolProvider provider = entry.getValue();
            McpSchema.Tool tool = provider.getToolDefinition();

            logger.info("注册 MCP 工具: {}", tool.getName());
            addTool(tool, provider::execute);
        }
        logger.info("内置 MCP 工具初始化完成");




        logger.info("正在初始化系统 Prompt 提示词...");
        Map<String, MCPPromptProvider> prompts = applicationContext.getBeansOfType(MCPPromptProvider.class);
        for (Map.Entry<String, MCPPromptProvider> entry : prompts.entrySet()) {
            MCPPromptProvider provider = entry.getValue();
            McpSchema.Prompt prompt = provider.getPromptDefinition();

            logger.info("注册 PROMPT 模版: {}", prompt.getName());
            addPrompt(prompt, provider::getPrompt);
        }
        logger.info("初始化系统 Prompt 提示词初始化完成");
    }


    private void addTool(McpSchema.Tool tool, BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call) {
        mcpServer.addTool(new McpServerFeatures.SyncToolSpecification(tool, call));
    }


    private void addPrompt(McpSchema.Prompt prompt, BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> call) {
        mcpServer.addPrompt(new McpServerFeatures.SyncPromptSpecification(prompt, call));
    }
}

