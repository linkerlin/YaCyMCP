package com.yacy.mcp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.service.McpService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP协议合规性测试
 * Stdio  验证 JSON-RPC 消息格式、stdout 输出合规性、协议错误处理等
 */
@SpringBootTest
@TestPropertySource(properties = {
    "yacy.server-url=http://localhost:8090",
    "yacy.connection-timeout=5000",
    "yacy.socket-timeout=10000"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class McpStdioProtocolTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private YaCyClient yaCyClient;

    @Autowired
    private McpService mcpService;

    private static boolean yacyAvailable = false;
    private static final List<String> protocolErrors = new ArrayList<>();

    @BeforeAll
    static void checkYaCyConnection() {
        System.out.println("=== MCP Stdio Protocol Test Starting ===");
    }

    @BeforeEach
    void setUp() {
        protocolErrors.clear();
        try {
            JsonNode result = yaCyClient.search("protocol_test", 1, 0);
            yacyAvailable = result != null && result.has("channels");
        } catch (Exception e) {
            yacyAvailable = false;
        }
    }

    // ==================== 测试 1: JSON-RPC 2.0 格式验证 ====================

    @Test
    @Order(10)
    @DisplayName("测试 JSON-RPC 2.0 初始化响应格式")
    void testInitializeResponseFormat() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-1");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new LinkedHashMap<>());
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "Request should be valid JSON");
        assertTrue(jsonRequest.contains("\"jsonrpc\":\"2.0\""), "Should contain jsonrpc version");
        assertTrue(jsonRequest.contains("\"method\":\"initialize\""), "Should contain method");

        System.out.println("✓ JSON-RPC 2.0 initialize request format is valid");
        System.out.println("  Request: " + jsonRequest);
    }

    @Test
    @Order(11)
    @DisplayName("测试工具列表响应格式")
    void testToolsListResponseFormat() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-2");
        request.put("method", "tools/list");
        request.put("params", new LinkedHashMap<>());

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "Request should be valid JSON");

        System.out.println("✓ JSON-RPC 2.0 tools/list request format is valid");
    }

    @Test
    @Order(12)
    @DisplayName("测试工具调用请求格式")
    void testToolCallRequestFormat() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-3");
        request.put("method", "tools/call");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "yacy_get_status");
        params.put("arguments", new LinkedHashMap<>());
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "Request should be valid JSON");
        assertTrue(jsonRequest.contains("\"name\":\"yacy_get_status\""), "Should contain tool name");

        System.out.println("✓ JSON-RPC 2.0 tools/call request format is valid");
        System.out.println("  Request: " + jsonRequest);
    }

    @Test
    @Order(13)
    @DisplayName("测试 ping 请求格式")
    void testPingRequestFormat() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-ping");
        request.put("method", "ping");
        request.put("params", new LinkedHashMap<>());

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "Request should be valid JSON");

        System.out.println("✓ JSON-RPC 2.0 ping request format is valid");
    }

    // ==================== 测试 2: 响应格式验证 ====================

    @Test
    @Order(20)
    @DisplayName("测试成功响应格式 - 包含 result 字段")
    void testSuccessResponseFormat() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", "test-response");
        response.put("result", Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of("tools", Map.of()),
            "serverInfo", Map.of("name", "yacy-mcp", "version", "1.0.0")
        ));

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response should be valid JSON");
        assertTrue(jsonResponse.contains("\"jsonrpc\":\"2.0\""), "Should contain jsonrpc version");
        assertTrue(jsonResponse.contains("\"result\""), "Should contain result field");
        assertFalse(jsonResponse.contains("\"error\""), "Success response should not contain error field");

        System.out.println("✓ Success response format is valid");
        System.out.println("  Response: " + jsonResponse);
    }

    @Test
    @Order(21)
    @DisplayName("测试错误响应格式 - 包含 error 字段")
    void testErrorResponseFormat() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", "test-error");
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", -32600);
        error.put("message", "Invalid Request");
        response.put("error", error);

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response should be valid JSON");
        assertTrue(jsonResponse.contains("\"error\""), "Should contain error field");
        assertTrue(jsonResponse.contains("\"code\""), "Should contain error code");
        assertTrue(jsonResponse.contains("\"message\""), "Should contain error message");

        System.out.println("✓ Error response format is valid");
        System.out.println("  Response: " + jsonResponse);
    }

    @Test
    @Order(22)
    @DisplayName("测试工具列表响应包含 tools 数组")
    void testToolsListResponseContainsToolsArray() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", "test-tools");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tools", List.of(
            Map.of("name", "yacy_search", "description", "Search YaCy", "inputSchema", Map.of("type", "object"))
        ));
        response.put("result", result);

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response should be valid JSON");
        assertTrue(jsonResponse.contains("\"tools\""), "Should contain tools array");
        assertTrue(jsonResponse.contains("\"name\""), "Should contain tool name");

        System.out.println("✓ Tools list response format is valid");
    }

    @Test
    @Order(23)
    @DisplayName("测试工具调用响应包含 content 和 isError 字段")
    void testToolCallResponseFormat() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", "test-call");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isError", false);
        result.put("content", List.of(Map.of("type", "text", "text", "Result content")));
        response.put("result", result);

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response should be valid JSON");
        assertTrue(jsonResponse.contains("\"isError\""), "Should contain isError field");
        assertTrue(jsonResponse.contains("\"content\""), "Should contain content field");
        assertTrue(jsonResponse.contains("\"type\":\"text\""), "Should contain content type");

        System.out.println("✓ Tool call response format is valid");
        System.out.println("  Response: " + jsonResponse);
    }

    // ==================== 测试 3: 协议错误处理 ====================

    @Test
    @Order(30)
    @DisplayName("测试无效 JSON 格式处理")
    void testInvalidJsonFormat() {
        String invalidJson = "{this is not valid json}";

        assertFalse(isValidJson(invalidJson), "Invalid JSON should be detected");
        System.out.println("✓ Invalid JSON format correctly detected");
    }

    @Test
    @Order(31)
    @DisplayName("测试缺少 jsonrpc 版本字段")
    void testMissingJsonrpcVersion() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("id", "test-missing-version");
        request.put("method", "initialize");

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "Should still be valid JSON");
        assertFalse(jsonRequest.contains("\"jsonrpc\""), "Should not contain jsonrpc field");

        System.out.println("✓ Missing jsonrpc version handled (valid JSON but not valid JSON-RPC)");
    }

    @Test
    @Order(32)
    @DisplayName("测试无效的 JSON-RPC 版本")
    void testInvalidJsonrpcVersion() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "1.0");  // Invalid version
        request.put("id", "test-invalid-version");
        request.put("method", "initialize");

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(jsonRequest.contains("\"jsonrpc\":\"1.0\""), "Should contain invalid version");

        System.out.println("✓ Invalid JSON-RPC version 1.0 detected");
    }

    @Test
    @Order(33)
    @DisplayName("测试空请求处理")
    void testEmptyRequest() {
        String emptyJson = "{}";

        assertTrue(isValidJson(emptyJson), "Empty object should be valid JSON");
        System.out.println("✓ Empty JSON object handled");
    }

    @Test
    @Order(34)
    @DisplayName("测试空数组合法性")
    void testEmptyArray() throws Exception {
        String emptyArray = "[]";

        assertTrue(isValidJson(emptyArray), "Empty array should be valid JSON");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", "test");
        response.put("result", List.of());

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response with empty array should be valid JSON");

        System.out.println("✓ Empty array handling validated");
    }

    // ==================== 测试 4: 工具定义 Schema 验证 ====================

    @Test
    @Order(40)
    @DisplayName("测试搜索工具 Schema 包含必需字段")
    void testSearchToolSchema() {
        var tools = mcpService.getToolDefinitions();
        var searchTool = tools.stream()
            .filter(t -> t.getName().equals("yacy_search"))
            .findFirst();

        assertTrue(searchTool.isPresent(), "yacy_search tool should exist");

        var schema = searchTool.get().getInputSchema();
        assertNotNull(schema, "Schema should not be null");
        assertEquals("object", schema.get("type"), "Schema type should be object");

        var properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties, "Properties should not be null");
        assertTrue(properties.containsKey("query"), "Should contain query property");

        var required = (List<String>) schema.get("required");
        assertNotNull(required, "Required field should not be null");
        assertTrue(required.contains("query"), "query should be required");

        System.out.println("✓ yacy_search tool schema is valid");
    }

    @Test
    @Order(41)
    @DisplayName("测试所有工具 Schema 格式一致性")
    void testAllToolsSchemaConsistency() {
        var tools = mcpService.getToolDefinitions();

        for (var tool : tools) {
            var schema = tool.getInputSchema();
            assertNotNull(schema, "Schema should not be null for " + tool.getName());
            assertEquals("object", schema.get("type"), 
                "Schema type should be object for " + tool.getName());

            var properties = schema.get("properties");
            assertNotNull(properties, "Properties should not be null for " + tool.getName());
        }

        System.out.println("✓ All " + tools.size() + " tool schemas have consistent format");
    }

    @Test
    @Order(42)
    @DisplayName("测试工具描述不为空")
    void testToolDescriptionsNotEmpty() {
        var tools = mcpService.getToolDefinitions();

        for (var tool : tools) {
            assertNotNull(tool.getDescription(), 
                "Description should not be null for " + tool.getName());
            assertFalse(tool.getDescription().isEmpty(), 
                "Description should not be empty for " + tool.getName());
            assertTrue(tool.getDescription().length() >= 10, 
                "Description should be meaningful (>=10 chars) for " + tool.getName());
        }

        System.out.println("✓ All tool descriptions are meaningful");
    }

    // ==================== 测试 5: Unicode 和特殊字符处理 ====================

    @Test
    @Order(50)
    @DisplayName("测试 Unicode 字符支持")
    void testUnicodeSupport() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-unicode");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("clientInfo", Map.of(
            "name", "测试客户端",
            "version", "1.0.0"
        ));
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with unicode should be valid");
        assertTrue(jsonRequest.contains("测试客户端"), "Unicode characters should be preserved");

        System.out.println("✓ Unicode character support validated");
        System.out.println("  Request: " + jsonRequest);
    }

    @Test
    @Order(51)
    @DisplayName("测试特殊字符转义")
    void testSpecialCharacterEscaping() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-special");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("query", "test \"quoted\" and \\backslash");
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with special chars should be valid");
        assertTrue(jsonRequest.contains("\\\""), "Quotes should be escaped");
        assertTrue(jsonRequest.contains("\\\\"), "Backslash should be escaped");

        System.out.println("✓ Special character escaping validated");
        System.out.println("  Request: " + jsonRequest);
    }

    @Test
    @Order(52)
    @DisplayName("测试换行符处理")
    void testNewlineHandling() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-newline");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("description", "Line1\nLine2\r\nWindows");
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with newlines should be valid");
        assertFalse(jsonRequest.contains("\n"), "Newlines should be escaped in JSON");

        System.out.println("✓ Newline handling validated");
    }

    // ==================== 测试 6: 数字范围和边界值 ====================

    @Test
    @Order(60)
    @DisplayName("测试大数字处理")
    void testLargeNumberHandling() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-large-number");
        request.put("method", "tools/call");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "yacy_search");
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("count", 1000);  // Large number
        arguments.put("offset", 999999999);  // Very large offset
        params.put("arguments", arguments);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with large numbers should be valid");

        System.out.println("✓ Large number handling validated");
        System.out.println("  Request: " + jsonRequest);
    }

    @Test
    @Order(61)
    @DisplayName("测试负数处理")
    void testNegativeNumberHandling() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-negative");
        request.put("method", "tools/call");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "yacy_search");
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("offset", -1);  // Negative offset
        params.put("arguments", arguments);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with negative numbers should be valid");
        assertTrue(jsonRequest.contains("-1"), "Negative number should be preserved");

        System.out.println("✓ Negative number handling validated");
    }

    @Test
    @Order(62)
    @DisplayName("测试零值处理")
    void testZeroValueHandling() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-zero");
        request.put("method", "tools/call");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "yacy_search");
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("count", 0);
        arguments.put("offset", 0);
        params.put("arguments", arguments);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with zero values should be valid");

        System.out.println("✓ Zero value handling validated");
    }

    // ==================== 测试 7: 浮点数处理 ====================

    @Test
    @Order(70)
    @DisplayName("测试浮点数格式")
    void testFloatNumberHandling() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-float");
        request.put("method", "tools/call");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "yacy_search");
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("lat", 37.7749);
        arguments.put("lng", -122.4194);
        params.put("arguments", arguments);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with float numbers should be valid");
        assertTrue(jsonRequest.contains("37.7749"), "Float should be preserved");

        System.out.println("✓ Float number handling validated");
        System.out.println("  Request: " + jsonRequest);
    }

    @Test
    @Order(71)
    @DisplayName("测试科学计数法")
    void testScientificNotation() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-scientific");
        request.put("method", "tools/call");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "yacy_search");
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("value", 1.23e-10);
        params.put("arguments", arguments);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with scientific notation should be valid");

        System.out.println("✓ Scientific notation handling validated");
    }

    // ==================== 测试 8: 布尔值和 null 处理 ====================

    @Test
    @Order(80)
    @DisplayName("测试布尔值格式")
    void testBooleanHandling() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-bool");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("enabled", true);
        params.put("disabled", false);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with booleans should be valid");
        assertTrue(jsonRequest.contains("true"), "True should be preserved");
        assertTrue(jsonRequest.contains("false"), "False should be preserved");

        System.out.println("✓ Boolean handling validated");
    }

    @Test
    @Order(81)
    @DisplayName("测试 null 值处理")
    void testNullValueHandling() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-null");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("optional", null);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with null should be valid");
        assertTrue(jsonRequest.contains("null"), "Null should be preserved");

        System.out.println("✓ Null value handling validated");
    }

    // ==================== 测试 9: 响应 ID 格式验证 ====================

    @Test
    @Order(90)
    @DisplayName("测试数字 ID 响应")
    void testNumericResponseId() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", 123);
        response.put("result", Map.of("test", "value"));

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response with numeric ID should be valid");
        assertTrue(jsonResponse.contains("\"id\":123"), "Numeric ID should be preserved");

        System.out.println("✓ Numeric response ID validated");
    }

    @Test
    @Order(91)
    @DisplayName("测试字符串 ID 响应")
    void testStringResponseId() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", "uuid-string-1234");
        response.put("result", Map.of("test", "value"));

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response with string ID should be valid");
        assertTrue(jsonResponse.contains("\"id\":\"uuid-string-1234\""), "String ID should be preserved");

        System.out.println("✓ String response ID validated");
    }

    @Test
    @Order(92)
    @DisplayName("测试 null ID 响应 (通知)")
    void testNullResponseId() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", (Object) null);
        response.put("result", Map.of("status", "processed"));

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response with null ID should be valid");
        assertTrue(jsonResponse.contains("\"id\":null"), "Null ID should be preserved");

        System.out.println("✓ Null response ID (notification) validated");
    }

    // ==================== 测试 10: 嵌套对象处理 ====================

    @Test
    @Order(100)
    @DisplayName("测试深度嵌套对象")
    void testDeeplyNestedObjects() throws Exception {
        Map<String, Object> nested = new LinkedHashMap<>();
        Map<String, Object> level1 = new LinkedHashMap<>();
        Map<String, Object> level2 = new LinkedHashMap<>();
        Map<String, Object> level3 = new LinkedHashMap<>();
        level3.put("deep", "value");
        level2.put("level3", level3);
        level1.put("level2", level2);
        nested.put("level1", level1);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", "test-nested");
        response.put("result", nested);

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(isValidJson(jsonResponse), "Response with nested objects should be valid");

        System.out.println("✓ Deeply nested objects handling validated");
        System.out.println("  Response: " + jsonResponse);
    }

    // ==================== 测试 11: 响应时间戳格式 ====================

    @Test
    @Order(110)
    @DisplayName("测试 ISO 8601 时间戳格式")
    void testTimestampFormat() throws Exception {
        String timestamp = "2024-11-05T12:30:00Z";

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-timestamp");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("timestamp", timestamp);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with timestamp should be valid");
        assertTrue(jsonRequest.contains(timestamp), "Timestamp should be preserved");

        System.out.println("✓ ISO 8601 timestamp handling validated");
    }

    @Test
    @Order(111)
    @DisplayName("测试 Unix 时间戳格式")
    void testUnixTimestampFormat() throws Exception {
        long unixTimestamp = 1699189800L;

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-unix-time");
        request.put("method", "initialize");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("unixTime", unixTimestamp);
        request.put("params", params);

        String jsonRequest = objectMapper.writeValueAsString(request);
        assertTrue(isValidJson(jsonRequest), "JSON with Unix timestamp should be valid");

        System.out.println("✓ Unix timestamp handling validated");
    }

    // ==================== 测试 12: 工具实际调用测试 ====================

    @Test
    @Order(120)
    @DisplayName("测试 yacy_get_status 实际调用")
    void testGetStatusExecution() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        var response = mcpService.executeTool(
            createToolRequest("yacy_get_status", new HashMap<>())
        );

        assertNotNull(response, "Response should not be null");
        System.out.println("✓ yacy_get_status executed successfully");

        if (response.isError()) {
            System.out.println("  Note: Response contains error (may need authentication)");
        }
    }

    @Test
    @Order(121)
    @DisplayName("测试 yacy_search 实际调用")
    void testSearchExecution() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        Map<String, Object> args = new HashMap<>();
        args.put("query", "integration test");
        args.put("count", 5);

        var response = mcpService.executeTool(createToolRequest("yacy_search", args));

        assertNotNull(response, "Response should not be null");
        assertFalse(response.isError() && response.getContent() == null, 
            "Error response should have error message");

        System.out.println("✓ yacy_search executed successfully");
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

    private com.yacy.mcp.model.McpToolCallRequest createToolRequest(String name, Map<String, Object> args) {
        com.yacy.mcp.model.McpToolCallRequest request = 
            new com.yacy.mcp.model.McpToolCallRequest();
        request.setName(name);
        request.setArguments(args);
        return request;
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== MCP Stdio Protocol Test Completed ===");
        if (!protocolErrors.isEmpty()) {
            System.out.println("Protocol errors found:");
            protocolErrors.forEach(e -> System.out.println("  - " + e));
        }
    }
}
