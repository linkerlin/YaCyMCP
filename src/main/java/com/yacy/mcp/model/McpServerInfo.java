package com.yacy.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP Server Info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpServerInfo {
    private String name;
    private String version;
    private List<String> capabilities;
}
