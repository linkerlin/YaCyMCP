package com.yacy.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.model.McpToolCallRequest;
import com.yacy.mcp.model.McpToolCallResponse;
import com.yacy.mcp.model.McpToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Service - Core service for handling MCP tool calls
 * Integrates with Spring AI Alibaba and AgentScope-Java
 */
@Service
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final YaCyClient yaCyClient;
    private final DatabaseService databaseService;
    private final ObjectMapper objectMapper;

    public McpService(YaCyClient yaCyClient, DatabaseService databaseService) {
        this.yaCyClient = yaCyClient;
        this.databaseService = databaseService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get all available MCP tool definitions
     */
    public List<McpToolDefinition> getToolDefinitions() {
        List<McpToolDefinition> tools = new ArrayList<>();

        // Search tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_search")
                .description("Search the YaCy index for documents matching a query")
                .inputSchema(createSearchSchema())
                .build());

        // Status tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_get_status")
                .description("Get YaCy server status information")
                .inputSchema(createEmptySchema())
                .build());

        // Network tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_get_network")
                .description("Get YaCy network information")
                .inputSchema(createEmptySchema())
                .build());

        // Crawl tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_start_crawl")
                .description("Start crawling a URL in YaCy")
                .inputSchema(createCrawlSchema())
                .build());

        // Index info tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_get_index_info")
                .description("Get YaCy index information")
                .inputSchema(createEmptySchema())
                .build());

        // Peers tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_get_peers")
                .description("Get YaCy peer information")
                .inputSchema(createEmptySchema())
                .build());

        // Performance tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_get_performance")
                .description("Get YaCy performance statistics")
                .inputSchema(createEmptySchema())
                .build());

        // Host browser tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_get_host_browser")
                .description("Browse hosts in YaCy index")
                .inputSchema(createHostBrowserSchema())
                .build());

        // Document tool
        tools.add(McpToolDefinition.builder()
                .name("yacy_get_document")
                .description("Get document details from YaCy index")
                .inputSchema(createDocumentSchema())
                .build());

        return tools;
    }

    /**
     * Execute a tool call
     */
    public McpToolCallResponse executeTool(McpToolCallRequest request) {
        String toolName = request.getName();
        Map<String, Object> args = request.getArguments();

        log.info("Executing tool: {} with args: {}", toolName, args);

        try {
            return switch (toolName) {
                case "yacy_search" -> executeSearch(args);
                case "yacy_get_status" -> executeGetStatus();
                case "yacy_get_network" -> executeGetNetwork();
                case "yacy_start_crawl" -> executeStartCrawl(args);
                case "yacy_get_index_info" -> executeGetIndexInfo();
                case "yacy_get_peers" -> executeGetPeers();
                case "yacy_get_performance" -> executeGetPerformance();
                case "yacy_get_host_browser" -> executeGetHostBrowser(args);
                case "yacy_get_document" -> executeGetDocument(args);
                default -> McpToolCallResponse.error("Unknown tool: " + toolName);
            };
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            return McpToolCallResponse.error("Error executing tool: " + e.getMessage());
        }
    }

    private McpToolCallResponse executeSearch(Map<String, Object> args) throws IOException {
        String query = (String) args.get("query");
        int count = args.containsKey("count") ? (int) args.get("count") : 10;
        int offset = args.containsKey("offset") ? (int) args.get("offset") : 0;

        long startTime = System.currentTimeMillis();
        JsonNode result = yaCyClient.search(query, count, offset);
        long duration = System.currentTimeMillis() - startTime;

        // Log search to database
        int resultCount = result.has("channels") ? result.get("channels").size() : 0;
        if (databaseService != null) {
            databaseService.logSearch(query, resultCount, duration);
        }

        return McpToolCallResponse.success(result);
    }

    private McpToolCallResponse executeGetStatus() throws IOException {
        return McpToolCallResponse.success(yaCyClient.getStatus());
    }

    private McpToolCallResponse executeGetNetwork() throws IOException {
        return McpToolCallResponse.success(yaCyClient.getNetworkInfo());
    }

    private McpToolCallResponse executeStartCrawl(Map<String, Object> args) throws IOException {
        String url = (String) args.get("url");
        int depth = args.containsKey("depth") ? (int) args.get("depth") : 0;

        JsonNode result = yaCyClient.startCrawl(url, depth);

        // Log crawl to database
        databaseService.logCrawl(url, depth, "started");

        return McpToolCallResponse.success(result);
    }

    private McpToolCallResponse executeGetIndexInfo() throws IOException {
        return McpToolCallResponse.success(yaCyClient.getIndexInfo());
    }

    private McpToolCallResponse executeGetPeers() throws IOException {
        return McpToolCallResponse.success(yaCyClient.getPeers());
    }

    private McpToolCallResponse executeGetPerformance() throws IOException {
        return McpToolCallResponse.success(yaCyClient.getPerformance());
    }

    private McpToolCallResponse executeGetHostBrowser(Map<String, Object> args) throws IOException {
        String host = (String) args.getOrDefault("host", "");
        int count = args.containsKey("count") ? (int) args.get("count") : 10;
        return McpToolCallResponse.success(yaCyClient.getHostBrowser(host, count));
    }

    private McpToolCallResponse executeGetDocument(Map<String, Object> args) throws IOException {
        String url = (String) args.get("url");
        return McpToolCallResponse.success(yaCyClient.getDocument(url));
    }

    // Schema creation helpers
    private Map<String, Object> createEmptySchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        return schema;
    }

    private Map<String, Object> createSearchSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("query", Map.of(
                "type", "string",
                "description", "Search query string"
        ));
        properties.put("count", Map.of(
                "type", "integer",
                "description", "Maximum number of results to return",
                "default", 10
        ));
        properties.put("offset", Map.of(
                "type", "integer",
                "description", "Start offset for pagination",
                "default", 0
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("query"));

        return schema;
    }

    private Map<String, Object> createCrawlSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("url", Map.of(
                "type", "string",
                "description", "URL to crawl"
        ));
        properties.put("depth", Map.of(
                "type", "integer",
                "description", "Crawl depth",
                "default", 0
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("url"));

        return schema;
    }

    private Map<String, Object> createHostBrowserSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("host", Map.of(
                "type", "string",
                "description", "Host to browse"
        ));
        properties.put("count", Map.of(
                "type", "integer",
                "description", "Maximum number of results",
                "default", 10
        ));

        schema.put("properties", properties);

        return schema;
    }

    private Map<String, Object> createDocumentSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("url", Map.of(
                "type", "string",
                "description", "Document URL"
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("url"));

        return schema;
    }
}
