package com.yacy.mcp.config;

import com.yacy.mcp.server.McpStdioServer;
import com.yacy.mcp.service.McpService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    public static final String MODE_MCP = "mcp";
    public static final String ENV_MCP_MODE = "MCP_MODE";
    public static final String ENV_DISABLE_MCP_STDIO = "DISABLE_MCP_STDIO";

    @Autowired
    private McpService mcpService;

    @Autowired
    private Environment environment;

    private McpStdioServer mcpServer;
    private boolean isMcpMode = false;

    @PostConstruct
    public void init() {
        isMcpMode = detectMcpMode();

        if (isMcpMode) {
            log.info("Starting MCP stdio server...");
            startMcpServer();
        } else {
            log.debug("MCP stdio disabled");
        }
    }

    private boolean detectMcpMode() {
        String disableEnv = System.getenv(ENV_DISABLE_MCP_STDIO);
        if (disableEnv != null && Boolean.parseBoolean(disableEnv)) {
            log.debug("MCP stdio disabled via {}", ENV_DISABLE_MCP_STDIO);
            return false;
        }

        String modeEnv = environment.getProperty(ENV_MCP_MODE);
        if (modeEnv != null && Boolean.parseBoolean(modeEnv)) {
            log.debug("MCP stdio enabled via config");
            return true;
        }

        String envValue = System.getenv(ENV_MCP_MODE);
        if (envValue != null && Boolean.parseBoolean(envValue)) {
            log.debug("MCP stdio enabled via env");
            return true;
        }

        String transport = System.getenv("MCP_TRANSPORT");
        if ("stdio".equalsIgnoreCase(transport)) {
            log.debug("MCP stdio enabled via MCP_TRANSPORT=stdio");
            return true;
        }

        log.debug("Defaulting to MCP stdio enabled");
        return true;
    }

    private void startMcpServer() {
        try {
            mcpServer = new McpStdioServer(mcpService);
            mcpServer.start();
            log.info("MCP stdio server ready - awaiting JSON-RPC messages on stdin/stdout");
        } catch (Exception e) {
            log.error("Failed to start MCP stdio server", e);
            throw new RuntimeException("Failed to start MCP stdio server", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (mcpServer != null) {
            mcpServer.stop();
        }
    }

    public boolean isMcpMode() {
        return isMcpMode;
    }
}
