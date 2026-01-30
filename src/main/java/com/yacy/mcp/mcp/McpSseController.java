package com.yacy.mcp.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP SSE端点控制器
 * 提供Server-Sent Events (SSE)端点用于MCP通信
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpSseController {

    private final McpServer mcpServer;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sessions = new ConcurrentHashMap<>();

    public McpSseController(McpServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    /**
     * SSE端点 - 建立MCP连接
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> mcpSse(@RequestParam(required = false) String sessionId) {
        log.info("New MCP SSE connection requested, sessionId: {}", sessionId);
        
        String effectiveSessionId = sessionId != null ? sessionId : generateSessionId();
        
        // Create a new sink for this session
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessions.put(effectiveSessionId, sink);
        
        // Use WebFlux transport to handle the MCP protocol
        WebFluxSseServerTransportProvider transport = new WebFluxSseServerTransportProvider.Builder()
            .build();
        
        // Connect the MCP server to this SSE endpoint
        // Note: This is a simplified version - full implementation would handle the transport properly
        
        return sink.asFlux()
            .doOnCancel(() -> {
                log.info("MCP SSE connection closed, sessionId: {}", effectiveSessionId);
                sessions.remove(effectiveSessionId);
            })
            .doOnError(error -> {
                log.error("Error in MCP SSE connection, sessionId: " + effectiveSessionId, error);
                sessions.remove(effectiveSessionId);
            });
    }

    /**
     * POST端点 - 接收MCP消息
     */
    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void handleMessage(
            @RequestParam String sessionId,
            @RequestBody String message) {
        log.info("Received MCP message for sessionId: {}", sessionId);
        
        Sinks.Many<ServerSentEvent<String>> sink = sessions.get(sessionId);
        if (sink != null) {
            // Process the message with MCP server and send response
            ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                .data(message)
                .build();
            sink.tryEmitNext(event);
        } else {
            log.warn("No active session found for sessionId: {}", sessionId);
        }
    }

    private String generateSessionId() {
        return "mcp-session-" + System.currentTimeMillis() + "-" + Math.random();
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "server", "YaCy MCP Service",
            "activeSessions", sessions.size()
        );
    }
}
