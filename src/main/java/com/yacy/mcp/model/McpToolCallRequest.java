package com.yacy.mcp.model;

import java.util.Map;

/**
 * MCP Tool Call Request
 */
public class McpToolCallRequest {
    private String name;
    private Map<String, Object> arguments;

    public McpToolCallRequest() {
    }

    public McpToolCallRequest(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
