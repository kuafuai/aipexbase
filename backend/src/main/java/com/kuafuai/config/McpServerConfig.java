package com.kuafuai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuafuai.manage.mcp.server.McpServer;
import com.kuafuai.manage.mcp.server.McpSyncServer;
import com.kuafuai.manage.mcp.server.transport.HttpServletSseServerTransportProvider;
import com.kuafuai.manage.mcp.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * MCP服务器配置类
 * 负责配置和启动MCP服务器
 */
@Configuration
public class McpServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

    @Value("${mcp.server.name:baas-mcp-server}")
    private String serverName;

    @Value("${mcp.server.version:1.0.0}")
    private String serverVersion;

    @Value("${mcp.server.message-endpoint:/mcp/message}")
    private String messageEndpoint;

    @Value("${mcp.server.sse-endpoint:/mcp/sse}")
    private String sseEndpoint;

    private McpSyncServer mcpServer;

    /**
     * 创建ObjectMapper Bean
     */
//    @Bean
//    public ObjectMapper objectMapper() {
//        return new ObjectMapper();
//    }

    /**
     * 创建传输提供者Bean
     */
    @Bean
    public HttpServletSseServerTransportProvider transportProvider(ObjectMapper objectMapper) {
        return new HttpServletSseServerTransportProvider(objectMapper, messageEndpoint, sseEndpoint);
    }

    /**
     * 创建MCP服务器Bean
     */
    @Bean
    public McpSyncServer mcpServer(ObjectMapper objectMapper, HttpServletSseServerTransportProvider transportProvider) {
        logger.info("正在初始化MCP服务器: {} v{}", serverName, serverVersion);
        
        // 创建并配置MCP同步服务器
        mcpServer = McpServer.sync(transportProvider)
                .serverInfo(serverName, serverVersion)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true) // 启用资源读写功能
                        .tools(true)          // 启用工具功能
                        .prompts(true)        // 当 prompt 发生动态变化时，通知给所有客户端
                        .logging()            // 启用日志功能
                        .build())
                .build();

        logger.info("MCP服务器初始化完成");
        return mcpServer;
    }

    /**
     * 注册MCP Servlet
     */
    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpServletRegistration(
            HttpServletSseServerTransportProvider transportProvider) {
        
        ServletRegistrationBean<HttpServletSseServerTransportProvider> registration = 
            new ServletRegistrationBean<>(transportProvider, messageEndpoint, sseEndpoint);
        
        registration.setName("mcpServlet");
        registration.setLoadOnStartup(1);
        
        logger.info("MCP Servlet已注册，消息端点: {}, SSE端点: {}", messageEndpoint, sseEndpoint);
        return registration;
    }

    /**
     * 应用启动后发送初始化日志
     */
    @PostConstruct
    public void sendInitializationLog() {
        if (mcpServer != null) {
            try {
                // 发送服务器初始化日志通知
                mcpServer.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                        .level(McpSchema.LoggingLevel.INFO)
                        .logger("mcp-server")
                        .data("MCP服务器已启动: " + serverName + " v" + serverVersion)
                        .build());
                
                logger.info("MCP服务器启动日志已发送");
            } catch (Exception e) {
                logger.error("发送MCP服务器初始化日志失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 应用关闭时优雅关闭MCP服务器
     */
    @PreDestroy
    public void shutdownMcpServer() {
        if (mcpServer != null) {
            try {
                logger.info("正在关闭MCP服务器...");
                mcpServer.closeGracefully();
                logger.info("MCP服务器已优雅关闭");
            } catch (Exception e) {
                logger.error("关闭MCP服务器时发生错误: {}", e.getMessage(), e);
            }
        }
    }
}
