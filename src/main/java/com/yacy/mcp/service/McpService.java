package com.yacy.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MCP Service implementing YaCy tools
 */
@Slf4j
@Service
public class McpService {

    private final YaCyClient yaCyClient;

    public McpService(YaCyClient yaCyClient) {
        this.yaCyClient = yaCyClient;
    }

    /**
     * Get list of available tools
     */
    public List<McpTool> listTools() {
        List<McpTool> tools = new ArrayList<>();

        // Search tool
        tools.add(new McpTool(
                "yacy_search",
                "Search the YaCy index for documents matching a query",
                createSchema(
                        Map.of(
                                "query", Map.of("type", "string", "description", "Search query"),
                                "count", Map.of("type", "integer", "description", "Maximum number of results", "default", 10),
                                "offset", Map.of("type", "integer", "description", "Start offset for results", "default", 0)
                        ),
                        List.of("query")
                )
        ));

        // Get status tool
        tools.add(new McpTool(
                "yacy_get_status",
                "Get YaCy server status and statistics",
                createSchema(Map.of(), List.of())
        ));

        // Get network info tool
        tools.add(new McpTool(
                "yacy_get_network",
                "Get YaCy network information and peer statistics",
                createSchema(Map.of(), List.of())
        ));

        // Start crawl tool
        tools.add(new McpTool(
                "yacy_start_crawl",
                "Start a new web crawl from a given URL",
                createSchema(
                        Map.of(
                                "url", Map.of("type", "string", "description", "Starting URL for the crawl"),
                                "depth", Map.of("type", "integer", "description", "Maximum crawl depth", "default", 3)
                        ),
                        List.of("url")
                )
        ));

        // Get index info tool
        tools.add(new McpTool(
                "yacy_get_index_info",
                "Get information about the YaCy search index",
                createSchema(Map.of(), List.of())
        ));

        // Get peers tool
        tools.add(new McpTool(
                "yacy_get_peers",
                "Get information about connected YaCy peers",
                createSchema(Map.of(), List.of())
        ));

        // Get performance tool
        tools.add(new McpTool(
                "yacy_get_performance",
                "Get YaCy performance statistics and queue information",
                createSchema(Map.of(), List.of())
        ));

        // Get host browser tool
        tools.add(new McpTool(
                "yacy_get_host_browser",
                "Browse hosts in the YaCy index",
                createSchema(
                        Map.of(
                                "path", Map.of("type", "string", "description", "Path to browse", "default", "")
                        ),
                        List.of()
                )
        ));

        // Get search result details
        tools.add(new McpTool(
                "yacy_get_document",
                "Get detailed information about a specific document",
                createSchema(
                        Map.of(
                                "urlhash", Map.of("type", "string", "description", "URL hash of the document")
                        ),
                        List.of("urlhash")
                )
        ));

        return tools;
    }

    /**
     * Execute a tool call
     */
    public McpToolCallResponse executeTool(McpToolCallRequest request) {
        try {
            log.info("Executing tool: {} with arguments: {}", request.getName(), request.getArguments());

            JsonNode result = switch (request.getName()) {
                case "yacy_search" -> {
                    String query = (String) request.getArguments().get("query");
                    int count = getIntArg(request.getArguments(), "count", 10);
                    int offset = getIntArg(request.getArguments(), "offset", 0);
                    yield yaCyClient.search(query, count, offset);
                }
                case "yacy_get_status" -> yaCyClient.getStatus();
                case "yacy_get_network" -> yaCyClient.getNetworkInfo();
                case "yacy_start_crawl" -> {
                    String url = (String) request.getArguments().get("url");
                    int depth = getIntArg(request.getArguments(), "depth", 3);
                    yield yaCyClient.startCrawl(url, depth);
                }
                case "yacy_get_index_info" -> yaCyClient.getIndexInfo();
                case "yacy_get_peers" -> yaCyClient.getPeers();
                case "yacy_get_performance" -> yaCyClient.getPerformance();
                case "yacy_get_host_browser" -> {
                    String path = (String) request.getArguments().getOrDefault("path", "");
                    yield yaCyClient.getHostBrowser(path);
                }
                case "yacy_get_document" -> {
                    String urlhash = (String) request.getArguments().get("urlhash");
                    yield yaCyClient.getSearchResult(urlhash);
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + request.getName());
            };

            return McpToolCallResponse.success(result);
        } catch (Exception e) {
            log.error("Error executing tool: " + request.getName(), e);
            return McpToolCallResponse.error(e.getMessage());
        }
    }

    private Map<String, Object> createSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private int getIntArg(Map<String, Object> args, String key, int defaultValue) {
        Object value = args.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return defaultValue;
    }
}
