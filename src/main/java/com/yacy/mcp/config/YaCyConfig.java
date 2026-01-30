package com.yacy.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for YaCy connection
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "yacy")
public class YaCyConfig {
    
    /**
     * YaCy server URL
     */
    private String serverUrl = "http://localhost:8090";
    
    /**
     * YaCy admin username
     */
    private String username = "admin";
    
    /**
     * YaCy admin password
     */
    private String password = "";
    
    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 30000;
    
    /**
     * Socket timeout in milliseconds
     */
    private int socketTimeout = 30000;
}
