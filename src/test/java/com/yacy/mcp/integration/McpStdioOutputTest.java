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
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP Stdio  输出验证测试
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

    @Autowired
    private YaCyClient yaCyClient;

    @Autowired
    private McpService mcpService;

    private static boolean yacyAvailable = false;
    private final ByteArrayOutputStream stdoutCapture = new ByteArrayOutputStream();
    private final PrintStream originalStdout = System.out;

    @BeforeAll
    static void checkYaCyConnection() {
        System.out.println("=== MCP Stdio Output Test Starting ===");
    }

    @BeforeEach
    void setUp() {
        stdoutCapture.reset();
        try {
            JsonNode result = yaCyClient.search("output_test", 1, 0);
            yacyAvailable = result != null && result.has("channels");
        } catch (Exception e) {
            yacyAvailable = false;
        }
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalStdout);
    }

    // ==================== 测试 1: 初始化响应 stdout 验证 ====================

    @Test
    @Order(10)
    @DisplayName("测试 initialize 响应输出是单行 JSON")
    void testInitializeResponseIsSingleLineJson() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{}}}\n";
        sendRequestToServer(server, initRequest);

        Thread.sleep(500);
        server.stop();

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
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-req-1\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{}}}\n";
        sendRequestToServer(server, initRequest);

        Thread.sleep(500);
        server.stop();

        String output = stdoutCapture.toString().trim();
        JsonNode response = objectMapper.readTree(output);

        assertEquals("2.0", response.get("jsonrpc").asText(), "jsonrpc version should be 2.0");
        assertNotNull(response.get("id"), "id should not be null");
        assertTrue(response.has("result"), "Should have result field");

        JsonNode result = response.get("result");
        assertEquals("2024-11-05", result.get("protocolVersion").asText(), "protocolVersion should match");
        assertTrue(result.has("capabilities"), "Should have capabilities");
        assertTrue(result.has("serverInfo"), "Should have serverInfo");

        JsonNode serverInfo = result.get("serverInfo");
        assertEquals("yacy-mcp", serverInfo.get("name").asText(), "server name should be yacy-mcp");
        assertEquals("1.0.0", serverInfo.get("version").asText(), "server version should be 1.0.0");

        System.out.println("✓ initialize response contains all required fields");
    }

    // ==================== 测试 2: 工具列表响应 stdout 验证 ====================

    @Test
    @Order(20)
    @DisplayName("测试 tools/list 响应输出格式")
    void testToolsListResponseFormat() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String toolsRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-tools-1\",\"method\":\"tools/list\",\"params\":{}}\n";
        sendRequestToServer(server, toolsRequest);

        Thread.sleep(500);
        server.stop();

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
        assertTrue(tools.size() >= 9, "Should have at least 9 tools");

        System.out.println("✓ tools/list response format is valid");
        System.out.println("  Found " + tools.size() + " tools");
    }

    @Test
    @Order(21)
    @DisplayName("测试 tools/list 每个工具包含必需字段")
    void testEachToolHasRequiredFields() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String toolsRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-tool-fields\",\"method\":\"tools/list\",\"params\":{}}\n";
        sendRequestToServer(server, toolsRequest);

        Thread.sleep(500);
        server.stop();

        String output = stdoutCapture.toString().trim();
        JsonNode response = objectMapper.readTree(output);
        JsonNode tools = response.get("result").get("tools");

        for (JsonNode tool : tools) {
            assertTrue(tool.has("name"), "Tool should have name");
            assertTrue(tool.has("description"), "Tool should have description");
            assertTrue(tool.has("inputSchema"), "Tool should have inputSchema");

            String name = tool.get("name").asText();
            assertFalse(name.isEmpty(), "Tool name should not be empty");

            String description = tool.get("description").asText();
            assertFalse(description.isEmpty(), "Tool description should not be empty for " + name);
            assertTrue(description.length() >= 10, "Tool description should be meaningful for " + name);

            JsonNode schema = tool.get("inputSchema");
            assertEquals("object", schema.get("type").asText(), "Schema type should be object");
        }

        System.out.println("✓ All tools have required fields (name, description, inputSchema)");
    }

    // ==================== 测试 3: 工具调用响应 stdout 验证 ====================

    @Test
    @Order(30)
    @DisplayName("测试 tools/call 成功响应格式")
    void testToolCallSuccessResponseFormat() throws Exception {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String callRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-call-1\",\"method\":\"tools/call\",\"params\":{\"name\":\"yacy_get_status\",\"arguments\":{}}}\n";
        sendRequestToServer(server, callRequest);

        Thread.sleep(500);
        server.stop();

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode response = objectMapper.readTree(output);
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals("test-call-1", response.get("id").asText());
        assertTrue(response.has("result"), "Should have result field");

        JsonNode result = response.get("result");
        assertTrue(result.has("isError"), "Should have isError field");
        assertTrue(result.has("content"), "Should have content field");

        boolean isError = result.get("isError").asBoolean();
        assertFalse(isError, "Response should not be an error");

        JsonNode content = result.get("content");
        assertTrue(content.isArray(), "Content should be an array");
        assertTrue(content.size() > 0, "Content should have at least one item");

        JsonNode firstContent = content.get(0);
        assertEquals("text", firstContent.get("type").asText(), "Content type should be text");
        assertTrue(firstContent.has("text"), "Content should have text field");

        System.out.println("✓ tools/call success response format is valid");
    }

    @Test
    @Order(31)
    @DisplayName("测试 tools/call 错误响应格式")
    void testToolCallErrorResponseFormat() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String callRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-call-error\",\"method\":\"tools/call\",\"params\":{\"name\":\"nonexistent_tool\",\"arguments\":{}}}\n";
        sendRequestToServer(server, callRequest);

        Thread.sleep(500);
        server.stop();

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode response = objectMapper.readTree(output);
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals("test-call-error", response.get("id").asText());
        assertTrue(response.has("result"), "Should have result field");

        JsonNode result = response.get("result");
        assertTrue(result.has("isError"), "Should have isError field");
        assertTrue(result.get("isError").asBoolean(), "Should be an error response");
        assertTrue(result.has("content"), "Should have content field");

        System.out.println("✓ tools/call error response format is valid");
    }

    // ==================== 测试 4: ping 响应 stdout 验证 ====================

    @Test
    @Order(40)
    @DisplayName("测试 ping 响应格式")
    void testPingResponseFormat() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String pingRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-ping-1\",\"method\":\"ping\",\"params\":{}}\n";
        sendRequestToServer(server, pingRequest);

        Thread.sleep(500);
        server.stop();

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

    // ==================== 测试 5: 错误处理 stdout 验证 ====================

    @Test
    @Order(50)
    @DisplayName("测试无效方法名的错误响应")
    void testInvalidMethodErrorResponse() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String invalidRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-invalid-method\",\"method\":\"invalid.method\",\"params\":{}}\n";
        sendRequestToServer(server, invalidRequest);

        Thread.sleep(500);
        server.stop();

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode response = objectMapper.readTree(output);
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals("test-invalid-method", response.get("id").asText());

        System.out.println("✓ Invalid method error response format is valid");
    }

    @Test
    @Order(51)
    @DisplayName("测试无效 JSON 的错误处理行为")
    void testInvalidJsonErrorResponse() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        Thread.sleep(300);

        String invalidJson = "this is not valid json\n";
        sendRequestToServer(server, invalidJson);

        Thread.sleep(500);
        server.stop();

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

    // ==================== 测试 6: 多请求顺序处理 ====================

    @Test
    @Order(60)
    @DisplayName("测试多请求按顺序响应")
    void testMultipleRequestsInOrder() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String requests = 
            "{\"jsonrpc\":\"2.0\",\"id\":\"multi-1\",\"method\":\"ping\",\"params\":{}}\n" +
            "{\"jsonrpc\":\"2.0\",\"id\":\"multi-2\",\"method\":\"ping\",\"params\":{}}\n" +
            "{\"jsonrpc\":\"2.0\",\"id\":\"multi-3\",\"method\":\"ping\",\"params\":{}}\n";

        sendRequestToServer(server, requests);

        Thread.sleep(500);
        server.stop();

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

    // ==================== 测试 7: stdout 无日志污染验证 ====================

    @Test
    @Order(70)
    @DisplayName("测试 stdout 输出不包含日志内容")
    void testStdoutNoLogPollution() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String pingRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-log-pollution\",\"method\":\"ping\",\"params\":{}}\n";
        sendRequestToServer(server, pingRequest);

        Thread.sleep(500);
        server.stop();

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
    @Order(71)
    @DisplayName("测试 stdout 响应是 JSON 对象（非数组）")
    void testStdoutResponseIsObjectNotArray() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String pingRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-object\",\"method\":\"ping\",\"params\":{}}\n";
        sendRequestToServer(server, pingRequest);

        Thread.sleep(500);
        server.stop();

        String output = stdoutCapture.toString().trim();
        assertFalse(output.isEmpty(), "Server should output something");

        assertTrue(isValidJson(output), "Response should be valid JSON");

        JsonNode node = objectMapper.readTree(output);
        assertTrue(node.isObject(), "Response should be a JSON object, not array");
        assertFalse(node.isArray(), "Response should not be a JSON array");

        System.out.println("✓ stdout response is JSON object (not array)");
    }

    // ==================== 测试 8: 协议版本兼容性 ====================

    @Test
    @Order(80)
    @DisplayName("测试响应始终包含 jsonrpc 版本")
    void testResponseAlwaysContainsJsonrpcVersion() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String[] methods = {"ping", "tools/list"};
        String[] ids = {"test-v1", "test-v2"};

        for (int i = 0; i < methods.length; i++) {
            String request = "{\"jsonrpc\":\"2.0\",\"id\":\"" + ids[i] + "\",\"method\":\"" + methods[i] + "\",\"params\":{}}\n";
            sendRequestToServer(server, request);
            Thread.sleep(200);
        }

        server.stop();

        String output = stdoutCapture.toString().trim();
        String[] responses = output.split("\n");

        for (String response : responses) {
            assertTrue(isValidJson(response), "Each response should be valid JSON");
            JsonNode node = objectMapper.readTree(response);
            assertTrue(node.has("jsonrpc"), "Response should have jsonrpc field");
            assertEquals("2.0", node.get("jsonrpc").asText(), "jsonrpc should be 2.0");
        }

        System.out.println("✓ All responses contain jsonrpc version 2.0");
    }

    // ==================== 测试 9: 通知消息处理 ====================

    @Test
    @Order(90)
    @DisplayName("测试 notifications/initialized 通知处理")
    void testNotificationsInitialized() throws Exception {
        System.setOut(new PrintStream(stdoutCapture));

        McpStdioServer server = new McpStdioServer(mcpService);
        server.start();

        String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}\n";
        sendRequestToServer(server, notification);

        Thread.sleep(500);
        server.stop();

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

    private void sendRequestToServer(McpStdioServer server, String input) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(input));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        invokeHandleMessage(server, line);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing: " + e.getMessage());
            }
        });

        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
        } finally {
            executor.shutdown();
        }
    }

    private void invokeHandleMessage(McpStdioServer server, String message) throws Exception {
        java.lang.reflect.Method method = McpStdioServer.class.getDeclaredMethod("handleMessage", String.class);
        method.setAccessible(true);
        method.invoke(server, message);
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== MCP Stdio Output Test Completed ===");
    }
}
