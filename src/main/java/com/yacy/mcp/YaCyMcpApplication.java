package com.yacy.mcp;

import com.yacy.mcp.config.McpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class YaCyMcpApplication {

    private static final Logger log = LoggerFactory.getLogger(YaCyMcpApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(YaCyMcpApplication.class, args);
        Environment env = context.getEnvironment();

        boolean mcpDisabled = System.getenv(McpServerConfig.ENV_DISABLE_MCP_STDIO) != null
                && Boolean.parseBoolean(System.getenv(McpServerConfig.ENV_DISABLE_MCP_STDIO));

        if (mcpDisabled) {
            log.info("YaCy MCP Service - stdio mode disabled");
        } else {
            log.info("YaCy MCP Service started - MCP stdio active on stdin/stdout");
        }
    }
}
