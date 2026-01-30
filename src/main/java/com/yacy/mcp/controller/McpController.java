package com.yacy.mcp.controller;

import com.yacy.mcp.config.McpConfig;
import com.yacy.mcp.model.*;
import com.yacy.mcp.service.McpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * REST Controller for MCP endpoints
 * @deprecated Use McpSseController with MCP SDK instead
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/mcp/legacy")
public class McpController {

    private final McpService mcpService;
    private final McpConfig mcpConfig;

    public McpController(McpService mcpService, McpConfig mcpConfig) {
        this.mcpService = mcpService;
        this.mcpConfig = mcpConfig;
    }

    /**
     * Get server information
     */
    @GetMapping("/info")
    public ResponseEntity<McpServerInfo> getServerInfo() {
        McpServerInfo info = new McpServerInfo(
                mcpConfig.getServerName(),
                mcpConfig.getServerVersion(),
                Arrays.asList("tools", "yacy-api")
        );
        return ResponseEntity.ok(info);
    }

    /**
     * List available tools
     */
    @GetMapping("/tools")
    public ResponseEntity<List<McpTool>> listTools() {
        return ResponseEntity.ok(mcpService.listTools());
    }

    /**
     * Execute a tool
     */
    @PostMapping("/tools/execute")
    public ResponseEntity<McpToolCallResponse> executeTool(@RequestBody McpToolCallRequest request) {
        log.info("Received tool execution request: {}", request);
        McpToolCallResponse response = mcpService.executeTool(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
