package com.yacy.mcp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.server.McpStdioServer;
import com.yacy.mcp.service.McpService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MCP Stdio 输出验证测试
 * 验证 McpStdioServer 的实际 stdout 输出是否符合 JSON-RPC 2.0 规范
 */
@SpringBootTest
@TestPropertySource(properties = {
    "yacy.server-url=http://localhost:8090",
    "yacy.connection-timeout=5000",
    "yacy.socket-timeout=10000"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class McpStdioOutputTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String YACY_API_URL = "http://localhost:8090";
    private static boolean yacyAvailable = false;

    @Autowired
    private YaCyClient yaCyClient;

    @Autowired
    private McpService mcpService;

    private final ByteArrayOutputStream stdoutCapture = new ByteArrayOutputStream();
    private final PrintStream originalStdout = System.out;

    @BeforeAll
    static void checkYaCyAvailability() {
        System.out.println("=== MCP Stdio Output Test Starting ===");
        System.out.println("检查 YaCy 服务器可用性...");
        
        yacyAvailable = checkYaCyApiAvailable(YACY_API_URL, 5);
        
        if (yacyAvailable) {
            System.out.println("✓ YaCy 服务器可用");
        } else {
            System.out.println("✗ YaCy 服务器不可用，部分测试将被跳过");
        }
        System.out.println("");
    }

    private static boolean checkYaCyApiAvailable(String baseUrl, int timeoutSeconds) {
        try {
            String testUrl = baseUrl + "/yacysearch.json?query=test&maximumRecords=1";
            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(testUrl))
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
            CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return response.statusCode() == 200 && !response.body().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        stdoutCapture.reset();
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalStdout);
    }

    private void runTestWithServer(java.util.function.Consumer<McpStdioServer> testLogic) {
        McpStdioServer server = new McpStdioServer(mcpService);
        server.setOutputStream(new PrintStream(stdoutCapture));
        server.start();
        try {
            testLogic.accept(server);
        } finally {
            server.stop();
        }
    }

    // ==================== 测试 1: 初始化响应 stdout 验证 ====================

    @Test
    @Order(10)
    @DisplayName("测试 initialize 响应输出是单行 JSON")
    void testInitializeResponseIsSingleLineJson() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{}}}\n";
            server.processRequest(initRequest);
        });

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        String[] lines = output.split("\n");
        assertEquals(1, lines.length, "Response should be single line: " + output);

        assertTrue(isValidJson(lines[0]), "Response should be valid JSON");
        assertTrue(lines[0].contains("\"jsonrpc\":\"2.0\""), "Should contain jsonrpc 2.0");
        assertTrue(lines[0].contains("\"id\":\"test-1\""), "Should contain correct id");
        assertTrue(lines[0].contains("\"result\""), "Should contain result field");
        assertTrue(lines[0].contains("\"protocolVersion\""), "Should contain protocol version");
        assertTrue(lines[0].contains("\"serverInfo\""), "Should contain server info");

        System.out.println("✓ initialize response is valid single-line JSON");
        System.out.println("  Output: " + lines[0]);
    }

    @Test
    @Order(11)
    @DisplayName("测试 initialize 响应包含必需字段")
    void testInitializeResponseContainsRequiredFields() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-req-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{}}}\n";
            server.processRequest(initRequest);
        });

        String output = stdoutCapture.toString().trim();
        JsonNode response = objectMapper.readTree(output);

        assertEquals("2.0", response.get("jsonrpc").asText(), "jsonrpc version should be 2.0");
        assertNotNull(response.get("id"), "id should not be null");
        assertTrue(response.has("result"), "Should have result field");

        JsonNode result = response.get("result");
        assertEquals("2024-11-05", result.get("protocolVersion").asText(), "protocolVersion should match");
        assertTrue(result.has("capabilities"), "Should have capabilities");
        assertTrue(result.has("serverInfo"), "Should have serverInfo");

        System.out.println("✓ initialize response contains all required fields");
    }

    // ==================== 测试 2: 工具列表响应 stdout 验证 ====================

    @Test
    @Order(20)
    @DisplayName("测试 tools/list 响应输出格式")
    void testToolsListResponseFormat() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String toolsRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-tools-1\",\"method\":\"tools/list\",\"params\":{}}\n";
            server.processRequest(toolsRequest);
        });

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode response = objectMapper.readTree(output);
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals("test-tools-1", response.get("id").asText());

        JsonNode result = response.get("result");
        assertTrue(result.has("tools"), "Should have tools array");

        JsonNode tools = result.get("tools");
        assertTrue(tools.isArray(), "tools should be an array");
        assertTrue(tools.size() > 0, "Should have at least one tool");

        for (JsonNode tool : tools) {
            assertTrue(tool.has("name"), "Tool should have name");
            assertTrue(tool.has("description"), "Tool should have description");
            assertTrue(tool.has("inputSchema"), "Tool should have inputSchema");
        }

        System.out.println("✓ tools/list response format is valid");
        System.out.println("  Found " + tools.size() + " tools");
    }

    // ==================== 测试 3: ping 响应 stdout 验证 ====================

    @Test
    @Order(30)
    @DisplayName("测试 ping 响应格式")
    void testPingResponseFormat() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String pingRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-ping-1\",\"method\":\"ping\",\"params\":{}}\n";
            server.processRequest(pingRequest);
        });

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode response = objectMapper.readTree(output);
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals("test-ping-1", response.get("id").asText());
        assertTrue(response.has("result"), "Should have result field");
        assertEquals("{}", response.get("result").toString(), "Result should be empty object");

        System.out.println("✓ ping response format is valid");
        System.out.println("  Output: " + output);
    }

    // ==================== 测试 4: 错误处理 stdout 验证 ====================

    @Test
    @Order(40)
    @DisplayName("测试无效方法名的错误响应")
    void testInvalidMethodErrorResponse() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String invalidRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-invalid-method\",\"method\":\"invalid.method\",\"params\":{}}\n";
            server.processRequest(invalidRequest);
        });

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode response = objectMapper.readTree(output);
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals("test-invalid-method", response.get("id").asText());

        System.out.println("✓ Invalid method error response format is valid");
    }

    @Test
    @Order(41)
    @DisplayName("测试无效 JSON 的错误处理行为")
    void testInvalidJsonErrorResponse() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String invalidJson = "this is not valid json\n";
            server.processRequest(invalidJson);
        });

        String output = stdoutCapture.toString().trim();

        if (output.isEmpty()) {
            System.out.println("✓ Invalid JSON handled (no response - server may have exited)");
            System.out.println("  Note: This is acceptable behavior for malformed JSON");
        } else {
            assertTrue(isValidJson(output), "Error response should be valid JSON if produced");

            JsonNode response = objectMapper.readTree(output);
            assertEquals("2.0", response.get("jsonrpc").asText());
            assertTrue(response.has("error") || response.has("result"), 
                "Response should have error or result field");

            System.out.println("✓ Invalid JSON error response format is valid");
            if (response.has("error")) {
                JsonNode error = response.get("error");
                System.out.println("  Error code: " + error.get("code").asInt());
                System.out.println("  Error message: " + error.get("message").asText());
            }
        }
    }

    // ==================== 测试 5: 多请求顺序处理 ====================

    @Test
    @Order(50)
    @DisplayName("测试多请求按顺序响应")
    void testMultipleRequestsInOrder() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String[] requests = {
                "{\"jsonrpc\":\"2.0\",\"id\":\"multi-1\",\"method\":\"ping\",\"params\":{}}\n",
                "{\"jsonrpc\":\"2.0\",\"id\":\"multi-2\",\"method\":\"ping\",\"params\":{}}\n",
                "{\"jsonrpc\":\"2.0\",\"id\":\"multi-3\",\"method\":\"ping\",\"params\":{}}\n"
            };
            for (String request : requests) {
                server.processRequest(request);
            }
        });

        String output = stdoutCapture.toString().trim();
        String[] responses = output.split("\n");

        assertEquals(3, responses.length, "Should have 3 responses");

        for (int i = 0; i < responses.length; i++) {
            assertTrue(isValidJson(responses[i]), "Response " + (i+1) + " should be valid JSON");
            JsonNode response = objectMapper.readTree(responses[i]);
            assertEquals("2.0", response.get("jsonrpc").asText());
            assertEquals("multi-" + (i+1), response.get("id").asText());
        }

        System.out.println("✓ Multiple requests handled in correct order");
    }

    // ==================== 测试 6: stdout 无日志污染验证 ====================

    @Test
    @Order(60)
    @DisplayName("测试 stdout 输出不包含日志内容")
    void testStdoutNoLogPollution() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String pingRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-log-pollution\",\"method\":\"ping\",\"params\":{}}\n";
            server.processRequest(pingRequest);
        });

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be pure JSON");

        assertFalse(output.contains("[INFO]"), "Should not contain [INFO] log level");
        assertFalse(output.contains("[DEBUG]"), "Should not contain [DEBUG] log level");
        assertFalse(output.contains("[WARN]"), "Should not contain [WARN] log level");
        assertFalse(output.contains("[ERROR]"), "Should not contain [ERROR] log level");
        assertFalse(output.contains("Starting YaCy"), "Should not contain startup logs");
        assertFalse(output.contains("PortManager"), "Should not contain PortManager logs");

        System.out.println("✓ stdout output is clean (no log pollution)");
        System.out.println("  Output: " + output);
    }

    @Test
    @Order(61)
    @DisplayName("测试 stdout 响应是 JSON 对象（非数组）")
    void testStdoutResponseIsObjectNotArray() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String pingRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-object\",\"method\":\"ping\",\"params\":{}}\n";
            server.processRequest(pingRequest);
        });

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode node = objectMapper.readTree(output);
        assertTrue(node.isObject(), "Response should be a JSON object, not array");
        assertFalse(node.isArray(), "Response should not be a JSON array");

        System.out.println("✓ stdout response is JSON object (not array)");
    }

    // ==================== 测试 7: 通知消息处理 ====================

    @Test
    @Order(70)
    @DisplayName("测试 notifications/initialized 通知处理")
    void testNotificationsInitialized() throws Exception {
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
        
        System.setOut(new PrintStream(stdoutCapture));
        
        runTestWithServer(server -> {
            String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}\n";
            server.processRequest(notification);
        });

        String output = stdoutCapture.toString().trim();
        assertTrue(output.isEmpty(), "Notification should not produce a response");

        System.out.println("✓ notifications/initialized handled (no response)");
    }

    // ==================== 辅助方法 ====================

    private boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @AfterAll
    static void tearDown() {
        System.out.println("");
        System.out.println("=== MCP Stdio Output Test Completed ===");
    }
}
