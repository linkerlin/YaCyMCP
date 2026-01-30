package com.yacy.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;
}
