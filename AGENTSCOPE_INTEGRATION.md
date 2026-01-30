# AgentScope-Java 集成指南

本文档说明如何将YaCy MCP服务与agentscope-java框架集成。

## 概述

YaCy MCP服务实现了标准的MCP（Model Context Protocol）接口，可以方便地与agentscope-java框架集成，为AI代理提供YaCy搜索引擎的全部功能。

## 架构设计

```
┌─────────────────────┐
│  AgentScope Agent   │
│   (Java Application)│
└──────────┬──────────┘
           │
           │ MCP Protocol
           │ (REST/JSON)
           ▼
┌─────────────────────┐
│   YaCy MCP Server   │
│  (Spring Boot App)  │
└──────────┬──────────┘
           │
           │ YaCy API
           │ (HTTP/JSON)
           ▼
┌─────────────────────┐
│    YaCy Server      │
│  (Search Engine)    │
└─────────────────────┘
```

## 集成步骤

### 1. 启动YaCy MCP服务

```bash
# 使用Maven
mvn spring-boot:run

# 或使用JAR包
java -jar target/yacy-mcp-1.0.0-SNAPSHOT.jar
```

### 2. 在AgentScope中配置MCP客户端

```java
import com.agentscope.mcp.McpClient;
import com.agentscope.mcp.McpConfig;

// 创建MCP客户端配置
McpConfig config = McpConfig.builder()
    .serverUrl("http://localhost:8990/mcp")
    .build();

// 创建MCP客户端
McpClient mcpClient = new McpClient(config);
```

### 3. 获取可用工具

```java
// 获取服务器信息
ServerInfo info = mcpClient.getServerInfo();
System.out.println("Server: " + info.getName() + " v" + info.getVersion());

// 列出所有可用工具
List<Tool> tools = mcpClient.listTools();
for (Tool tool : tools) {
    System.out.println("Tool: " + tool.getName() + " - " + tool.getDescription());
}
```

### 4. 执行工具调用

```java
// 搜索示例
Map<String, Object> searchArgs = new HashMap<>();
searchArgs.put("query", "artificial intelligence");
searchArgs.put("count", 10);

ToolCallResponse response = mcpClient.executeTool("yacy_search", searchArgs);
if (!response.isError()) {
    JsonNode results = (JsonNode) response.getContent();
    // 处理搜索结果
    processSearchResults(results);
}

// 启动爬虫示例
Map<String, Object> crawlArgs = new HashMap<>();
crawlArgs.put("url", "https://example.com");
crawlArgs.put("depth", 2);

ToolCallResponse crawlResponse = mcpClient.executeTool("yacy_start_crawl", crawlArgs);
```

## 在AgentScope中创建自定义Agent

### 搜索Agent示例

```java
import com.agentscope.agent.Agent;
import com.agentscope.message.Message;

public class YaCySearchAgent extends Agent {
    
    private final McpClient mcpClient;
    
    public YaCySearchAgent(String name, McpClient mcpClient) {
        super(name);
        this.mcpClient = mcpClient;
    }
    
    @Override
    public Message execute(Message input) {
        String query = input.getContent();
        
        // 执行搜索
        Map<String, Object> args = Map.of(
            "query", query,
            "count", 20
        );
        
        ToolCallResponse response = mcpClient.executeTool("yacy_search", args);
        
        if (response.isError()) {
            return new Message("Error: " + response.getContent());
        }
        
        // 处理和格式化结果
        JsonNode results = (JsonNode) response.getContent();
        String formattedResults = formatSearchResults(results);
        
        return new Message(formattedResults);
    }
    
    private String formatSearchResults(JsonNode results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search Results:\n");
        
        JsonNode channels = results.get("channels");
        if (channels != null && channels.isArray()) {
            for (JsonNode channel : channels) {
                JsonNode items = channel.get("items");
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        sb.append("\n- ").append(item.get("title").asText());
                        sb.append("\n  URL: ").append(item.get("link").asText());
                        sb.append("\n  ").append(item.get("description").asText());
                        sb.append("\n");
                    }
                }
            }
        }
        
        return sb.toString();
    }
}
```

### 网页爬虫Agent示例

```java
public class YaCyCrawlAgent extends Agent {
    
    private final McpClient mcpClient;
    
    public YaCyCrawlAgent(String name, McpClient mcpClient) {
        super(name);
        this.mcpClient = mcpClient;
    }
    
    @Override
    public Message execute(Message input) {
        // 从消息中解析URL和深度
        String url = extractUrl(input.getContent());
        int depth = extractDepth(input.getContent());
        
        // 启动爬虫
        Map<String, Object> args = Map.of(
            "url", url,
            "depth", depth
        );
        
        ToolCallResponse response = mcpClient.executeTool("yacy_start_crawl", args);
        
        if (response.isError()) {
            return new Message("Crawl failed: " + response.getContent());
        }
        
        return new Message("Crawl started for " + url + " with depth " + depth);
    }
}
```

## 多Agent协作示例

```java
import com.agentscope.pipeline.Pipeline;
import com.agentscope.pipeline.SequentialPipeline;

public class YaCyAgentPipeline {
    
    public static void main(String[] args) {
        // 创建MCP客户端
        McpClient mcpClient = new McpClient(
            McpConfig.builder()
                .serverUrl("http://localhost:8990/mcp")
                .build()
        );
        
        // 创建多个Agent
        YaCySearchAgent searchAgent = new YaCySearchAgent("searcher", mcpClient);
        YaCyCrawlAgent crawlAgent = new YaCyCrawlAgent("crawler", mcpClient);
        YaCyAnalyzerAgent analyzerAgent = new YaCyAnalyzerAgent("analyzer", mcpClient);
        
        // 创建Pipeline
        Pipeline pipeline = new SequentialPipeline(
            searchAgent,    // 先搜索相关内容
            crawlAgent,     // 然后爬取找到的页面
            analyzerAgent   // 最后分析爬取的结果
        );
        
        // 执行Pipeline
        Message input = new Message("Find and analyze information about quantum computing");
        Message result = pipeline.execute(input);
        
        System.out.println("Final result: " + result.getContent());
    }
}
```

## 异步处理

对于长时间运行的操作（如爬虫），建议使用异步处理：

```java
import java.util.concurrent.CompletableFuture;

public class AsyncYaCyAgent extends Agent {
    
    private final McpClient mcpClient;
    private final ExecutorService executor;
    
    public AsyncYaCyAgent(String name, McpClient mcpClient) {
        super(name);
        this.mcpClient = mcpClient;
        this.executor = Executors.newFixedThreadPool(10);
    }
    
    public CompletableFuture<Message> executeAsync(Message input) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> args = parseInput(input);
            ToolCallResponse response = mcpClient.executeTool("yacy_start_crawl", args);
            return new Message(formatResponse(response));
        }, executor);
    }
}
```

## 错误处理

```java
public Message executeWithRetry(Message input) {
    int maxRetries = 3;
    int retryCount = 0;
    
    while (retryCount < maxRetries) {
        try {
            Map<String, Object> args = parseInput(input);
            ToolCallResponse response = mcpClient.executeTool("yacy_search", args);
            
            if (!response.isError()) {
                return new Message(formatResponse(response));
            }
            
            // 如果是错误响应，重试
            retryCount++;
            Thread.sleep(1000 * retryCount); // 指数退避
            
        } catch (Exception e) {
            retryCount++;
            if (retryCount >= maxRetries) {
                return new Message("Error after " + maxRetries + " retries: " + e.getMessage());
            }
        }
    }
    
    return new Message("Max retries exceeded");
}
```

## 配置管理

建议使用配置文件管理MCP客户端设置：

```yaml
# agentscope-config.yml
mcp:
  yacy:
    server-url: http://localhost:8990/mcp
    timeout: 30000
    retry-count: 3
    
agents:
  search:
    enabled: true
    max-results: 20
  crawl:
    enabled: true
    default-depth: 3
```

```java
// 加载配置
Config config = ConfigLoader.load("agentscope-config.yml");

// 使用配置创建客户端
McpClient mcpClient = new McpClient(
    McpConfig.builder()
        .serverUrl(config.getMcp().getYacy().getServerUrl())
        .timeout(config.getMcp().getYacy().getTimeout())
        .build()
);
```

## 监控和日志

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoredYaCyAgent extends Agent {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoredYaCyAgent.class);
    private final McpClient mcpClient;
    
    @Override
    public Message execute(Message input) {
        logger.info("Executing YaCy search: {}", input.getContent());
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> args = parseInput(input);
            ToolCallResponse response = mcpClient.executeTool("yacy_search", args);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Search completed in {} ms", duration);
            
            if (response.isError()) {
                logger.error("Search failed: {}", response.getContent());
            }
            
            return new Message(formatResponse(response));
            
        } catch (Exception e) {
            logger.error("Exception during search", e);
            return new Message("Error: " + e.getMessage());
        }
    }
}
```

## 最佳实践

1. **连接池管理**: 使用连接池来管理与MCP服务器的连接
2. **缓存策略**: 对频繁的搜索查询实施缓存
3. **超时设置**: 为长时间运行的操作设置合适的超时时间
4. **错误处理**: 实现完善的错误处理和重试机制
5. **日志记录**: 记录所有MCP调用以便调试和监控
6. **异步处理**: 对耗时操作使用异步处理避免阻塞
7. **资源清理**: 确保正确关闭连接和释放资源

## 示例项目

完整的示例项目可以在以下位置找到：
- GitHub: https://github.com/linkerlin/YaCyMCP/examples/agentscope-integration

## 参考资料

- [AgentScope文档](https://agentscope.io/docs)
- [MCP协议规范](https://modelcontextprotocol.io)
- [YaCy API文档](https://yacy.net/api)
