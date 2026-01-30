package com.yacy.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for MCP server
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mcp")
public class McpConfig {
    
    /**
     * MCP server name
     */
    private String serverName = "YaCy MCP Service";
    
    /**
     * MCP server version
     */
    private String serverVersion = "1.0.0";
    
    /**
     * MCP server port
     */
    private int serverPort = 8080;
    
    /**
     * Enable agent scope integration
     */
    private boolean agentScopeEnabled = true;
}
