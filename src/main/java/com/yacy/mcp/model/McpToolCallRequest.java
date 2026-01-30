package com.yacy.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP Tool Call Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpToolCallRequest {
    private String name;
    private Map<String, Object> arguments;
}
