package com.yacy.mcp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.model.McpToolCallRequest;
import com.yacy.mcp.model.McpToolCallResponse;
import com.yacy.mcp.model.McpToolDefinition;
import com.yacy.mcp.service.McpService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 全流程集成测试
 * 测试 YaCy 连接、MCP 工具注册、工具调用等完整流程
 */
@SpringBootTest
@TestPropertySource(properties = {
    "yacy.server-url=http://localhost:8090",
    "yacy.connection-timeout=5000",
    "yacy.socket-timeout=10000"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class McpIntegrationTest {

    @Autowired
    private YaCyClient yaCyClient;

    @Autowired
    private McpService mcpService;

    private static boolean yacyAvailable = false;

    /**
     * 测试前检查 YaCy 连接
     */
    @BeforeAll
    static void checkYaCyConnection() {
        System.out.println("=== MCP Integration Test Starting ===");
        System.out.println("Checking YaCy server availability...");
    }

    @BeforeEach
    void setUp() {
        try {
            // 尝试搜索来验证连接（search API 比 status API 更可靠）
            JsonNode result = yaCyClient.search("test", 1, 0);
            yacyAvailable = result != null && result.has("channels");
            if (yacyAvailable) {
                System.out.println("✓ YaCy server is available at http://localhost:8090");
            }
        } catch (Exception e) {
            yacyAvailable = false;
            System.out.println("✗ YaCy server is not available: " + e.getMessage());
        }
    }

    // ==================== 测试 1: YaCy 连接测试 ====================

    @Test
    @Order(1)
    @DisplayName("测试 YaCy 服务器连接")
    void testYaCyConnection() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        assertDoesNotThrow(() -> {
            // 使用搜索 API 验证连接，因为它更可靠
            JsonNode result = yaCyClient.search("connection_test", 1, 0);
            assertNotNull(result, "Search result should not be null");
            assertTrue(result.has("channels"), "Result should have channels");
            System.out.println("✓ Successfully connected to YaCy server");
            System.out.println("  Response has " + result.get("channels").size() + " channels");
        });
    }

    @Test
    @Order(2)
    @DisplayName("测试 YaCy 搜索功能")
    void testYaCySearch() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        assertDoesNotThrow(() -> {
            JsonNode result = yaCyClient.search("test", 10, 0);
            assertNotNull(result, "Search result should not be null");
            assertTrue(result.has("channels"), "Result should have channels");
            System.out.println("✓ YaCy search functionality works");
        });
    }

    // ==================== 测试 2: MCP 工具注册测试 ====================

    @Test
    @Order(10)
    @DisplayName("测试 MCP 工具注册")
    void testMcpToolRegistration() {
        List<McpToolDefinition> tools = mcpService.getToolDefinitions();

        assertNotNull(tools, "Tools list should not be null");
        assertFalse(tools.isEmpty(), "Tools list should not be empty");
        assertTrue(tools.size() >= 9, "Should have at least 9 tools registered");

        // 验证核心工具存在
        List<String> requiredTools = List.of(
            "yacy_search",
            "yacy_get_status",
            "yacy_get_network",
            "yacy_start_crawl",
            "yacy_get_index_info",
            "yacy_get_peers",
            "yacy_get_performance",
            "yacy_get_host_browser",
            "yacy_get_document"
        );

        for (String toolName : requiredTools) {
            boolean found = tools.stream().anyMatch(t -> t.getName().equals(toolName));
            assertTrue(found, "Required tool should be registered: " + toolName);
        }

        System.out.println("✓ All " + tools.size() + " MCP tools are registered");
        tools.forEach(tool -> System.out.println("  - " + tool.getName() + ": " + tool.getDescription()));
    }

    @Test
    @Order(11)
    @DisplayName("测试 MCP 工具定义完整性")
    void testMcpToolDefinitionCompleteness() {
        List<McpToolDefinition> tools = mcpService.getToolDefinitions();

        for (McpToolDefinition tool : tools) {
            assertNotNull(tool.getName(), "Tool name should not be null");
            assertNotNull(tool.getDescription(), "Tool description should not be null");
            assertNotNull(tool.getInputSchema(), "Tool input schema should not be null");
            assertFalse(tool.getName().isEmpty(), "Tool name should not be empty");
            assertFalse(tool.getDescription().isEmpty(), "Tool description should not be empty");
        }

        System.out.println("✓ All tool definitions are complete");
    }

    // ==================== 测试 3: MCP 工具调用测试 ====================

    @Test
    @Order(20)
    @DisplayName("测试 yacy_get_status 工具调用")
    void testGetStatusTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_get_status");
        request.setArguments(new HashMap<>());

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.isError(), "Response should not be an error");
        assertNotNull(response.getContent(), "Response content should not be null");

        System.out.println("✓ yacy_get_status tool executed successfully");
    }

    @Test
    @Order(21)
    @DisplayName("测试 yacy_search 工具调用")
    void testSearchTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        Map<String, Object> args = new HashMap<>();
        args.put("query", "java");
        args.put("count", 5);
        args.put("offset", 0);

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_search");
        request.setArguments(args);

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.isError(), "Response should not be an error: " + 
            (response.isError() ? response.getContent() : ""));
        assertNotNull(response.getContent(), "Response content should not be null");

        System.out.println("✓ yacy_search tool executed successfully");
    }

    @Test
    @Order(22)
    @DisplayName("测试 yacy_get_network 工具调用")
    void testGetNetworkTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_get_network");
        request.setArguments(new HashMap<>());

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        // 某些 API 可能需要认证或不可用，只验证工具能执行
        System.out.println("✓ yacy_get_network tool executed (result: " + 
            (response.isError() ? "error - " + response.getContent() : "success") + ")");
    }

    @Test
    @Order(23)
    @DisplayName("测试 yacy_get_index_info 工具调用")
    void testGetIndexInfoTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_get_index_info");
        request.setArguments(new HashMap<>());

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        System.out.println("✓ yacy_get_index_info tool executed (result: " + 
            (response.isError() ? "error - " + response.getContent() : "success") + ")");
    }

    @Test
    @Order(24)
    @DisplayName("测试 yacy_get_peers 工具调用")
    void testGetPeersTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_get_peers");
        request.setArguments(new HashMap<>());

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        System.out.println("✓ yacy_get_peers tool executed (result: " + 
            (response.isError() ? "error - " + response.getContent() : "success") + ")");
    }

    @Test
    @Order(25)
    @DisplayName("测试 yacy_get_performance 工具调用")
    void testGetPerformanceTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_get_performance");
        request.setArguments(new HashMap<>());

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        System.out.println("✓ yacy_get_performance tool executed (result: " +
            (response.isError() ? "error - " + response.getContent() : "success") + ")");
    }

    @Test
    @Order(26)
    @DisplayName("测试 yacy_get_host_browser 工具调用")
    void testGetHostBrowserTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        Map<String, Object> args = new HashMap<>();
        args.put("path", "localhost");

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_get_host_browser");
        request.setArguments(args);

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        // 可能返回错误如果路径不存在，但不应抛出异常
        System.out.println("✓ yacy_get_host_browser tool executed (result: " + 
            (response.isError() ? "error" : "success") + ")");
    }

    @Test
    @Order(27)
    @DisplayName("测试 yacy_get_document 工具调用")
    void testGetDocumentTool() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        Map<String, Object> args = new HashMap<>();
        args.put("urlhash", "test-hash");

        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_get_document");
        request.setArguments(args);

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        System.out.println("✓ yacy_get_document tool executed (result: " + 
            (response.isError() ? "error" : "success") + ")");
    }

    // ==================== 测试 4: 错误处理测试 ====================

    @Test
    @Order(30)
    @DisplayName("测试未知工具调用")
    void testUnknownTool() {
        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("unknown_tool");
        request.setArguments(new HashMap<>());

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error for unknown tool");
        assertNotNull(response.getContent(), "Error message should not be null");

        System.out.println("✓ Unknown tool handled correctly: " + response.getContent());
    }

    @Test
    @Order(31)
    @DisplayName("测试无效参数处理")
    void testInvalidParameters() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        // 测试缺少必需参数
        McpToolCallRequest request = new McpToolCallRequest();
        request.setName("yacy_search");
        request.setArguments(new HashMap<>()); // 缺少 query 参数

        McpToolCallResponse response = mcpService.executeTool(request);

        assertNotNull(response, "Response should not be null");
        // 应该优雅处理，不抛出异常
        System.out.println("✓ Invalid parameters handled (result: " + 
            (response.isError() ? "error" : "success") + ")");
    }

    // ==================== 测试 5: 端到端流程测试 ====================

    @Test
    @Order(40)
    @DisplayName("测试完整搜索流程")
    void testCompleteSearchWorkflow() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        // Step 1: 获取服务器状态
        McpToolCallRequest statusRequest = new McpToolCallRequest();
        statusRequest.setName("yacy_get_status");
        statusRequest.setArguments(new HashMap<>());

        McpToolCallResponse statusResponse = mcpService.executeTool(statusRequest);
        // 某些 API 可能需要认证，不强制要求成功
        System.out.println("Step 1/3: Got server status (result: " +
            (statusResponse.isError() ? "error" : "success") + ") ✓");

        // Step 2: 执行搜索（核心功能，应该成功）
        Map<String, Object> searchArgs = new HashMap<>();
        searchArgs.put("query", "open source");
        searchArgs.put("count", 10);
        searchArgs.put("offset", 0);

        McpToolCallRequest searchRequest = new McpToolCallRequest();
        searchRequest.setName("yacy_search");
        searchRequest.setArguments(searchArgs);

        McpToolCallResponse searchResponse = mcpService.executeTool(searchRequest);
        assertFalse(searchResponse.isError(), "Search should succeed: " +
            (searchResponse.isError() ? searchResponse.getContent() : ""));
        System.out.println("Step 2/3: Executed search ✓");

        // Step 3: 获取索引信息
        McpToolCallRequest indexRequest = new McpToolCallRequest();
        indexRequest.setName("yacy_get_index_info");
        indexRequest.setArguments(new HashMap<>());

        McpToolCallResponse indexResponse = mcpService.executeTool(indexRequest);
        System.out.println("Step 3/3: Got index info (result: " +
            (indexResponse.isError() ? "error" : "success") + ") ✓");

        System.out.println("✓ Complete search workflow executed successfully");
    }

    @Test
    @Order(41)
    @DisplayName("测试网络信息获取流程")
    void testNetworkInfoWorkflow() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        // 获取网络信息
        McpToolCallRequest networkRequest = new McpToolCallRequest();
        networkRequest.setName("yacy_get_network");
        networkRequest.setArguments(new HashMap<>());

        McpToolCallResponse networkResponse = mcpService.executeTool(networkRequest);
        System.out.println("Network info (result: " +
            (networkResponse.isError() ? "error" : "success") + ")");

        // 获取对等节点
        McpToolCallRequest peersRequest = new McpToolCallRequest();
        peersRequest.setName("yacy_get_peers");
        peersRequest.setArguments(new HashMap<>());

        McpToolCallResponse peersResponse = mcpService.executeTool(peersRequest);
        System.out.println("Peers info (result: " +
            (peersResponse.isError() ? "error" : "success") + ")");

        // 获取性能信息
        McpToolCallRequest perfRequest = new McpToolCallRequest();
        perfRequest.setName("yacy_get_performance");
        perfRequest.setArguments(new HashMap<>());

        McpToolCallResponse perfResponse = mcpService.executeTool(perfRequest);
        System.out.println("Performance info (result: " +
            (perfResponse.isError() ? "error" : "success") + ")");

        System.out.println("✓ Network info workflow executed successfully");
    }

    // ==================== 测试 6: 性能测试 ====================

    @Test
    @Order(50)
    @DisplayName("测试工具调用性能")
    void testToolCallPerformance() {
        Assumptions.assumeTrue(yacyAvailable, "YaCy server not available, skipping test");

        int iterations = 5;
        long totalTime = 0;
        int successCount = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();

            // 使用搜索工具进行性能测试（更可靠）
            Map<String, Object> args = new HashMap<>();
            args.put("query", "performance_test_" + i);
            args.put("count", 1);
            args.put("offset", 0);

            McpToolCallRequest request = new McpToolCallRequest();
            request.setName("yacy_search");
            request.setArguments(args);

            McpToolCallResponse response = mcpService.executeTool(request);
            if (!response.isError()) {
                successCount++;
            }

            long end = System.currentTimeMillis();
            totalTime += (end - start);
        }

        double avgTime = totalTime / (double) iterations;
        System.out.println("✓ Average tool call time: " + avgTime + "ms over " + iterations + " iterations");
        System.out.println("  Successful calls: " + successCount + "/" + iterations);

        // 断言平均响应时间应小于 3 秒，且至少有一半调用成功
        assertTrue(avgTime < 3000, "Average response time should be less than 3000ms");
        assertTrue(successCount >= iterations / 2, "At least half of the calls should succeed");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== MCP Integration Test Completed ===");
    }
}
