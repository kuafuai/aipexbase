package com.kuafuai.manage.mcp.spec;


import javax.servlet.AsyncContext;

/**
 * Marker interface for the server-side MCP transport.
 *
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 */
public interface McpServerTransport extends McpTransport {

    AsyncContext getAsyncContext();
}
