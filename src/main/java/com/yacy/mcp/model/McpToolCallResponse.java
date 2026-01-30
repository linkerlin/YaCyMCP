package com.yacy.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Tool Call Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpToolCallResponse {
    private boolean isError;
    private Object content;

    public static McpToolCallResponse success(Object content) {
        return new McpToolCallResponse(false, content);
    }

    public static McpToolCallResponse error(String message) {
        return new McpToolCallResponse(true, message);
    }
}
