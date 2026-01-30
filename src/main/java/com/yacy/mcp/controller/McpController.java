package com.yacy.mcp.controller;

import com.yacy.mcp.model.McpToolCallRequest;
import com.yacy.mcp.model.McpToolCallResponse;
import com.yacy.mcp.model.McpToolDefinition;
import com.yacy.mcp.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP REST API Controller
 * Provides endpoints for MCP tool discovery and execution
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * Get server information and available tools
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        log.info("Getting MCP server info");
        return Map.of(
                "name", "YaCy MCP Server",
                "version", "1.0.0",
                "description", "MCP server providing YaCy search engine API functionalities",
                "tools", mcpService.getToolDefinitions()
        );
    }

    /**
     * Get all available tools
     */
    @GetMapping("/tools")
    public List<McpToolDefinition> getTools() {
        log.info("Getting available MCP tools");
        return mcpService.getToolDefinitions();
    }

    /**
     * Execute a tool call
     */
    @PostMapping("/tools/call")
    public McpToolCallResponse callTool(@RequestBody McpToolCallRequest request) {
        log.info("Received tool call: {}", request.getName());
        return mcpService.executeTool(request);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy");
    }
}
