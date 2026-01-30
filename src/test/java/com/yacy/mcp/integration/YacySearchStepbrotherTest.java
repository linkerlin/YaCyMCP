package com.yacy.mcp.integration;

import com.yacy.mcp.config.YaCyConfig;
import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.service.McpService;
import com.yacy.mcp.service.DatabaseService;
import com.yacy.mcp.model.McpToolCallRequest;
import com.yacy.mcp.model.McpToolCallResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for searching "步子哥" using yacy_search tool
 * Note: Requires YaCy server running on localhost:8090
 */
public class YacySearchStepbrotherTest {

    private static final String YACY_API_URL = "http://localhost:8090";
    private static final String TEST_QUERY = "steper";
    private static volatile boolean yacyAvailable = false;
    
    @BeforeAll
    static void checkYaCyAvailability() {
        System.out.println("=== 检查 YaCy 服务器可用性 ===");
        System.out.println("测试端点: " + YACY_API_URL + "/yacysearch.json?query=" + TEST_QUERY);
        System.out.println("");
        
        yacyAvailable = checkYaCyApiAvailable(YACY_API_URL, TEST_QUERY, 5);
        
        if (yacyAvailable) {
            System.out.println("✓ YaCy 服务器可用");
        } else {
            System.out.println("✗ YaCy 服务器不可用");
            System.out.println("  请确保 YaCy 服务器运行在 " + YACY_API_URL);
            System.out.println("  或设置环境变量 YACY_API_URL");
        }
        System.out.println("");
    }
    
    private static boolean checkYaCyApiAvailable(String baseUrl, String query, int timeoutSeconds) {
        try {
            String testUrl = baseUrl + "/yacysearch.json?query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("  发送测试请求: " + testUrl);
            
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
            
            int statusCode = response.statusCode();
            String body = response.body();
            
            System.out.println("  响应状态码: " + statusCode);
            System.out.println("  响应内容长度: " + body.length() + " 字符");
            
            if (statusCode == 200 && !body.isEmpty()) {
                System.out.println("  ✓ API 返回有效响应");
                return true;
            } else {
                System.out.println("  ✗ API 返回无效响应");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  ✗ 连接失败: " + e.getMessage());
            return false;
        }
    }

    @Test
    @Order(1)
    @DisplayName("搜索测试: 步子哥")
    void testSearchStepbrother() {
        System.out.println("=== 搜索测试: 步子哥 ===");
        
        // Skip if YaCy is not available
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过此测试");
        
        try {
            // Create YaCy config
            YaCyConfig config = new YaCyConfig();
            config.setServerUrl(YACY_API_URL);
            config.setConnectionTimeout(30000);
            config.setSocketTimeout(30000);
            
            // Create YaCy client
            YaCyClient yaCyClient = new YaCyClient(config);
            
            // Create database service (null for test)
            DatabaseService databaseService = null;
            
            // Create MCP service
            McpService mcpService = new McpService(yaCyClient, databaseService);
            
            // Build search request for "步子哥"
            McpToolCallRequest request = new McpToolCallRequest();
            request.setName("yacy_search");
            request.setArguments(Map.of(
                "query", "步子哥",
                "count", 10,
                "offset", 0
            ));
            
            System.out.println("搜索查询: 步子哥");
            System.out.println("参数: count=10, offset=0");
            System.out.println("");
            System.out.println("正在执行搜索...");
            
            // Execute search
            McpToolCallResponse response = mcpService.executeTool(request);
            
            // Print results
            System.out.println("");
            System.out.println("搜索完成!");
            System.out.println("响应状态: " + (response.isError() ? "错误 ❌" : "成功 ✓"));
            
            if (response.isError()) {
                System.out.println("错误信息: " + response.getContent());
                fail("搜索返回错误: " + response.getContent());
            } else {
                assertNotNull(response.getContent(), "内容不应为空");
                
                String content = response.getContent().toString();
                System.out.println("响应内容类型: " + response.getContent().getClass().getSimpleName());
                System.out.println("");
                System.out.println("响应内容:");
                if (content.length() > 500) {
                    System.out.println(content.substring(0, 500) + "...");
                } else {
                    System.out.println(content);
                }
            }
            
            System.out.println("");
            System.out.println("✓ 步子哥搜索测试完成");
            
        } catch (Exception e) {
            System.out.println("");
            System.out.println("✗ 测试失败: " + e.getMessage());
            fail("测试失败: " + e.getMessage(), e);
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("搜索测试: steper")
    void testSearchSteper() {
        System.out.println("=== 搜索测试: steper ===");
        
        assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过此测试");
        
        try {
            YaCyConfig config = new YaCyConfig();
            config.setServerUrl(YACY_API_URL);
            config.setConnectionTimeout(30000);
            config.setSocketTimeout(30000);
            
            YaCyClient yaCyClient = new YaCyClient(config);
            DatabaseService databaseService = null;
            McpService mcpService = new McpService(yaCyClient, databaseService);
            
            McpToolCallRequest request = new McpToolCallRequest();
            request.setName("yacy_search");
            request.setArguments(Map.of(
                "query", "steper",
                "count", 5,
                "offset", 0
            ));
            
            System.out.println("搜索查询: steper");
            System.out.println("参数: count=5, offset=0");
            System.out.println("");
            System.out.println("正在执行搜索...");
            
            McpToolCallResponse response = mcpService.executeTool(request);
            
            System.out.println("");
            System.out.println("搜索完成!");
            System.out.println("响应状态: " + (response.isError() ? "错误 ❌" : "成功 ✓"));
            
            if (response.isError()) {
                System.out.println("错误信息: " + response.getContent());
                fail("搜索返回错误: " + response.getContent());
            } else {
                assertNotNull(response.getContent(), "内容不应为空");
                
                String content = response.getContent().toString();
                System.out.println("响应内容长度: " + content.length() + " 字符");
                System.out.println("");
                System.out.println("响应内容预览:");
                if (content.length() > 500) {
                    System.out.println(content.substring(0, 500) + "...");
                } else {
                    System.out.println(content);
                }
            }
            
            System.out.println("");
            System.out.println("✓ steper 搜索测试完成");
            
        } catch (Exception e) {
            System.out.println("");
            System.out.println("✗ 测试失败: " + e.getMessage());
            fail("测试失败: " + e.getMessage(), e);
        }
    }
}
