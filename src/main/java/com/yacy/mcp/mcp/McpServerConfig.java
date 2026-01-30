package com.yacy.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yacy.mcp.client.YaCyClient;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * MCP服务器配置 - 使用MCP SDK构建标准MCP服务器
 */
@Slf4j
@Configuration
public class McpServerConfig {

    private final YaCyClient yaCyClient;
    private final ObjectMapper objectMapper;

    public McpServerConfig(YaCyClient yaCyClient) {
        this.yaCyClient = yaCyClient;
        this.objectMapper = new ObjectMapper();
    }

    @Bean
    public McpSyncServer mcpServer() {
        // Create transport provider
        WebFluxSseServerTransportProvider transportProvider = new WebFluxSseServerTransportProvider.Builder()
            .build();
        
        // Create MCP server with all YaCy tools
        return McpServer.sync(transportProvider)
            .serverInfo("YaCy MCP Service", "1.0.0")
            .tool(createYaCySearchTool(), this::executeYaCySearch)
            .tool(createYaCyGetStatusTool(), this::executeYaCyGetStatus)
            .tool(createYaCyGetNetworkTool(), this::executeYaCyGetNetwork)
            .tool(createYaCyStartCrawlTool(), this::executeYaCyStartCrawl)
            .tool(createYaCyGetIndexInfoTool(), this::executeYaCyGetIndexInfo)
            .tool(createYaCyGetPeersTool(), this::executeYaCyGetPeers)
            .tool(createYaCyGetPerformanceTool(), this::executeYaCyGetPerformance)
            .tool(createYaCyGetHostBrowserTool(), this::executeYaCyGetHostBrowser)
            .tool(createYaCyGetDocumentTool(), this::executeYaCyGetDocument)
            .build();
    }

    // Tool definitions
    private McpSchema.Tool createYaCySearchTool() {
        return McpSchema.Tool.builder()
            .name("yacy_search")
            .description("Search the YaCy index for documents matching a query")
            .inputSchema(createSchema(Map.of(
                "query", Map.of("type", "string", "description", "Search query"),
                "count", Map.of("type", "integer", "description", "Maximum number of results", "default", 10),
                "offset", Map.of("type", "integer", "description", "Start offset for results", "default", 0)
            ), List.of("query")))
            .build();
    }

    private McpSchema.Tool createYaCyGetStatusTool() {
        return McpSchema.Tool.builder()
            .name("yacy_get_status")
            .description("Get YaCy server status and statistics")
            .inputSchema(createSchema(Map.of(), List.of()))
            .build();
    }

    private McpSchema.Tool createYaCyGetNetworkTool() {
        return McpSchema.Tool.builder()
            .name("yacy_get_network")
            .description("Get YaCy network information and peer statistics")
            .inputSchema(createSchema(Map.of(), List.of()))
            .build();
    }

    private McpSchema.Tool createYaCyStartCrawlTool() {
        return McpSchema.Tool.builder()
            .name("yacy_start_crawl")
            .description("Start a new web crawl from a given URL")
            .inputSchema(createSchema(Map.of(
                "url", Map.of("type", "string", "description", "Starting URL for the crawl"),
                "depth", Map.of("type", "integer", "description", "Maximum crawl depth", "default", 3)
            ), List.of("url")))
            .build();
    }

    private McpSchema.Tool createYaCyGetIndexInfoTool() {
        return McpSchema.Tool.builder()
            .name("yacy_get_index_info")
            .description("Get information about the YaCy search index")
            .inputSchema(createSchema(Map.of(), List.of()))
            .build();
    }

    private McpSchema.Tool createYaCyGetPeersTool() {
        return McpSchema.Tool.builder()
            .name("yacy_get_peers")
            .description("Get information about connected YaCy peers")
            .inputSchema(createSchema(Map.of(), List.of()))
            .build();
    }

    private McpSchema.Tool createYaCyGetPerformanceTool() {
        return McpSchema.Tool.builder()
            .name("yacy_get_performance")
            .description("Get YaCy performance statistics and queue information")
            .inputSchema(createSchema(Map.of(), List.of()))
            .build();
    }

    private McpSchema.Tool createYaCyGetHostBrowserTool() {
        return McpSchema.Tool.builder()
            .name("yacy_get_host_browser")
            .description("Browse hosts in the YaCy index")
            .inputSchema(createSchema(Map.of(
                "path", Map.of("type", "string", "description", "Path to browse", "default", "")
            ), List.of()))
            .build();
    }

    private McpSchema.Tool createYaCyGetDocumentTool() {
        return McpSchema.Tool.builder()
            .name("yacy_get_document")
            .description("Get detailed information about a specific document")
            .inputSchema(createSchema(Map.of(
                "urlhash", Map.of("type", "string", "description", "URL hash of the document")
            ), List.of("urlhash")))
            .build();
    }

    // Tool execution handlers
    private McpSchema.CallToolResult executeYaCySearch(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            String query = getStringArg(arguments, "query");
            if (query == null || query.isEmpty()) {
                return createErrorResult("Query parameter is required");
            }
            int count = getIntArg(arguments, "count", 10);
            int offset = getIntArg(arguments, "offset", 0);
            
            JsonNode result = yaCyClient.search(query, count, offset);
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_search", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyGetStatus(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            JsonNode result = yaCyClient.getStatus();
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_get_status", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyGetNetwork(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            JsonNode result = yaCyClient.getNetworkInfo();
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_get_network", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyStartCrawl(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            String url = getStringArg(arguments, "url");
            if (url == null || url.isEmpty()) {
                return createErrorResult("URL parameter is required");
            }
            int depth = getIntArg(arguments, "depth", 3);
            
            JsonNode result = yaCyClient.startCrawl(url, depth);
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_start_crawl", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyGetIndexInfo(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            JsonNode result = yaCyClient.getIndexInfo();
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_get_index_info", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyGetPeers(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            JsonNode result = yaCyClient.getPeers();
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_get_peers", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyGetPerformance(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            JsonNode result = yaCyClient.getPerformance();
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_get_performance", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyGetHostBrowser(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            String path = getStringArg(arguments, "path");
            JsonNode result = yaCyClient.getHostBrowser(path != null ? path : "");
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_get_host_browser", e);
            return createErrorResult(e.getMessage());
        }
    }

    private McpSchema.CallToolResult executeYaCyGetDocument(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> arguments) {
        try {
            String urlhash = getStringArg(arguments, "urlhash");
            if (urlhash == null || urlhash.isEmpty()) {
                return createErrorResult("URL hash parameter is required");
            }
            JsonNode result = yaCyClient.getSearchResult(urlhash);
            return createSuccessResult(result);
        } catch (Exception e) {
            log.error("Error executing yacy_get_document", e);
            return createErrorResult(e.getMessage());
        }
    }

    // Helper methods
    private McpSchema.CallToolResult createSuccessResult(JsonNode result) throws IOException {
        List<McpSchema.Content> content = List.of(
            new McpSchema.TextContent(objectMapper.writeValueAsString(result))
        );
        return new McpSchema.CallToolResult(content, false);
    }

    private McpSchema.CallToolResult createErrorResult(String message) {
        List<McpSchema.Content> content = List.of(
            new McpSchema.TextContent(message)
        );
        return new McpSchema.CallToolResult(content, true);
    }

    private McpSchema.JsonSchema createSchema(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema("object", properties, required, null, null, null);
    }

    private String getStringArg(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value != null ? value.toString() : null;
    }

    private int getIntArg(Map<String, Object> args, String key, int defaultValue) {
        Object value = args.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for {}: {}, using default: {}", key, value, defaultValue);
                return defaultValue;
            }
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
