package com.yacy.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yacy.mcp.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class McpStdioServer {

    private static final Logger log = LoggerFactory.getLogger(McpStdioServer.class);

    private final McpService mcpService;
    private final ObjectMapper objectMapper;
    private final PrintStream stdout;
    private final BufferedReader stdin;
    private volatile boolean running = false;
    private ExecutorService executor;

    public McpStdioServer(McpService mcpService) {
        this.mcpService = mcpService;
        this.objectMapper = new ObjectMapper();
        this.stdout = new PrintStream(new BufferedOutputStream(System.out), true);
        this.stdin = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        log.info("Starting MCP Stdio Server...");
        running = true;
        executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                log.info("MCP Stdio Server listening for JSON-RPC messages...");
                String line;
                while (running && (line = stdin.readLine()) != null) {
                    if (!line.isEmpty()) {
                        try {
                            handleMessage(line);
                        } catch (Exception e) {
                            log.error("Error handling message: {}", line, e);
                            sendError(null, -32600, "Invalid JSON: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Error reading from stdin", e);
                }
            }
        });

        log.info("MCP Stdio Server started successfully");
    }

    private void handleMessage(String line) throws Exception {
        JsonNode json = objectMapper.readTree(line);
        String jsonrpc = json.has("jsonrpc") ? json.get("jsonrpc").asText() : null;

        if (!"2.0".equals(jsonrpc)) {
            sendError(null, -32600, "Invalid JSON-RPC version");
            return;
        }

        String method = json.has("method") ? json.get("method").asText() : null;
        JsonNode id = json.has("id") ? json.get("id") : null;
        JsonNode params = json.has("params") ? json.get("params") : null;

        log.debug("Received JSON-RPC request: method={}, id={}", method, id);

        switch (method) {
            case "initialize" -> handleInitialize(id, params);
            case "notifications/initialized" -> handleInitialized();
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, params);
            case "ping" -> handlePing(id);
            default -> sendError(id, -32601, "Method not found: " + method);
        }
    }

    private void handleInitialize(JsonNode id, JsonNode params) {
        log.info("MCP Client initializing...");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of(
                "tools", Map.of()
        ));
        result.put("serverInfo", Map.of(
                "name", "yacy-mcp",
                "version", "1.0.0"
        ));

        sendResponse(id, result);
        log.info("MCP Initialize response sent");
    }

    private void handleInitialized() {
        log.info("MCP Client ready");
    }

    private void handleToolsList(JsonNode id) {
        log.info("Listing tools...");

        List<Map<String, Object>> tools = new ArrayList<>();
        for (var tool : mcpService.getToolDefinitions()) {
            tools.add(Map.of(
                    "name", tool.getName(),
                    "description", tool.getDescription(),
                    "inputSchema", tool.getInputSchema()
            ));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tools", tools);

        sendResponse(id, result);
        log.info("Sent {} tools", tools.size());
    }

    private void handleToolsCall(JsonNode id, JsonNode params) {
        String toolName = params.has("name") ? params.get("name").asText() : null;
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : null;

        log.info("Tool call: {} with args: {}", toolName, arguments);

        if (toolName == null) {
            sendError(id, -32600, "Missing tool name");
            return;
        }

        try {
            com.yacy.mcp.model.McpToolCallRequest request = new com.yacy.mcp.model.McpToolCallRequest();
            request.setName(toolName);

            Map<String, Object> args = new HashMap<>();
            if (arguments != null && arguments.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = arguments.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    args.put(entry.getKey(), jsonToObject(entry.getValue()));
                }
            }
            request.setArguments(args);

            com.yacy.mcp.model.McpToolCallResponse response = mcpService.executeTool(request);

            Map<String, Object> result = new LinkedHashMap<>();

            if (response.isError()) {
                result.put("isError", true);
                result.put("content", List.of(Map.of(
                        "type", "text",
                        "text", response.getContent() != null ? response.getContent().toString() : "Unknown error"
                )));
            } else {
                result.put("isError", false);
                String content = response.getContent() != null ?
                        objectMapper.writeValueAsString(response.getContent()) : "";
                result.put("content", List.of(Map.of(
                        "type", "text",
                        "text", content
                )));
            }

            sendResponse(id, result);

        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            sendError(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    private void handlePing(JsonNode id) {
        sendResponse(id, Map.of());
    }

    private Object jsonToObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            if (node.canConvertToInt() && node.asText().indexOf('.') < 0 && node.asText().indexOf('e') < 0 && node.asText().indexOf('E') < 0) {
                return node.asInt();
            }
            if (node.canConvertToLong() && node.asText().indexOf('.') < 0 && node.asText().indexOf('e') < 0 && node.asText().indexOf('E') < 0) {
                return node.asLong();
            }
            return node.asDouble();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(jsonToObject(item));
            }
            return list;
        }
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                map.put(entry.getKey(), jsonToObject(entry.getValue()));
            }
            return map;
        }
        return node.toString();
    }

    private void sendResponse(JsonNode id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id != null ? id : UUID.randomUUID().toString());
        response.put("result", result);

        String json = toJson(response);
        stdout.println(json);
        stdout.flush();
        log.debug("Sent response: {}", json.substring(0, Math.min(200, json.length())));
    }

    private void sendError(JsonNode id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id != null ? id : UUID.randomUUID().toString());
        response.put("error", Map.of(
                "code", code,
                "message", message
        ));

        String json = toJson(response);
        stdout.println(json);
        stdout.flush();
        log.debug("Sent error: {}", json);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error serializing JSON", e);
            return "{}";
        }
    }

    public void stop() {
        log.info("Stopping MCP Stdio Server...");
        running = false;

        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("MCP Stdio Server stopped");
    }
}
