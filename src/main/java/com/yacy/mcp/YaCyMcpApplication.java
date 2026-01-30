package com.yacy.mcp;

import com.yacy.mcp.config.McpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.web.ServerProperties;

@SpringBootApplication
public class YaCyMcpApplication {

    private static final Logger log = LoggerFactory.getLogger(YaCyMcpApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(YaCyMcpApplication.class, args);
        Environment env = context.getEnvironment();

        boolean mcpDisabled = System.getenv(McpServerConfig.ENV_DISABLE_MCP_STDIO) != null
                && Boolean.parseBoolean(System.getenv(McpServerConfig.ENV_DISABLE_MCP_STDIO));

        if (mcpDisabled) {
            log.info("YaCy MCP Service started as web server only (MCP stdio disabled)");
        } else {
            log.info("YaCy MCP Service started - MCP stdio active on stdin/stdout");
            log.info("Web server running on port 8990 for health checks");
        }
    }

    @Configuration
    public static class McpModeConfig {

        @Bean
        @Primary
        public ServerProperties serverProperties() {
            return new ServerProperties();
        }
    }
}
