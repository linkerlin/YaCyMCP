package com.yacy.mcp.model;

/**
 * MCP Tool Call Response
 */
public class McpToolCallResponse {
    private boolean error;
    private Object content;

    public McpToolCallResponse() {
    }

    public McpToolCallResponse(boolean error, Object content) {
        this.error = error;
        this.content = content;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public static McpToolCallResponse success(Object content) {
        return new McpToolCallResponse(false, content);
    }

    public static McpToolCallResponse error(String message) {
        return new McpToolCallResponse(true, message);
    }
}
