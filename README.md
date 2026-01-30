# YaCyMCP

一个纯Java实现的MCP（Model Context Protocol）服务，包含YaCy的几乎全部的API功能。

## 技术栈

- **框架**: Spring Boot 3.2.1
- **AI集成**: Spring AI Alibaba 1.1.0.0 (Agent Framework + DashScope)
- **Agent运行时**: AgentScope-Java 1.0.8
- **MCP协议**: MCP Spring WebFlux 0.10.0
- **数据库**: SQLite 3.44.1 + jOOQ 3.18.7
- **HTTP客户端**: Apache HttpClient 5.3

## 功能特性

### YaCy API工具

本服务提供了以下YaCy API的MCP工具封装：

1. **yacy_search** - 搜索YaCy索引
   - 参数：query（查询字符串）、count（结果数量）、offset（偏移量）
   
2. **yacy_get_status** - 获取YaCy服务器状态
   
3. **yacy_get_network** - 获取YaCy网络信息
   
4. **yacy_start_crawl** - 启动新的网页爬虫
   - 参数：url（起始URL）、depth（爬取深度）
   
5. **yacy_get_index_info** - 获取搜索索引信息
   
6. **yacy_get_peers** - 获取对等节点信息
   
7. **yacy_get_performance** - 获取性能统计
   
8. **yacy_get_host_browser** - 浏览主机索引
   - 参数：path（浏览路径）
   
9. **yacy_get_document** - 获取文档详细信息
   - 参数：urlhash（文档URL哈希）

## 快速开始

### 前置要求

- Java 17或更高版本
- Maven 3.6+
- YaCy服务器运行在 http://localhost:8090

### 构建项目

```bash
mvn clean package
```

### 运行服务

```bash
java -jar target/yacy-mcp-1.0.0-SNAPSHOT.jar
```

或者使用Maven：

```bash
mvn spring-boot:run
```

服务将在 http://localhost:8080 启动。

### 配置

编辑 `src/main/resources/application.yml` 来配置：

```yaml
# YaCy配置
yacy:
  server-url: http://localhost:8090  # YaCy服务器地址
  username: admin                     # YaCy管理员用户名
  password: ""                        # YaCy管理员密码

# Spring AI Alibaba配置
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}     # DashScope API密钥
      enabled: ${DASHSCOPE_ENABLED:false} # 启用DashScope AI功能

# MCP配置
mcp:
  server-name: YaCy MCP Service
  server-version: 1.0.0
```

**注意**：如果要使用Spring AI Alibaba的AI功能，需要：
1. 获取阿里云DashScope API密钥
2. 设置环境变量：`export DASHSCOPE_API_KEY=your-key-here`
3. 启用DashScope：`export DASHSCOPE_ENABLED=true`

## API端点

### MCP端点

- `GET /mcp/info` - 获取服务器信息
- `GET /mcp/tools` - 列出所有可用工具
- `POST /mcp/tools/execute` - 执行工具调用
- `GET /mcp/health` - 健康检查

### 示例请求

#### 列出所有工具

```bash
curl http://localhost:8080/mcp/tools
```

#### 执行搜索

```bash
curl -X POST http://localhost:8080/mcp/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "yacy_search",
    "arguments": {
      "query": "java programming",
      "count": 10,
      "offset": 0
    }
  }'
```

#### 启动爬虫

```bash
curl -X POST http://localhost:8080/mcp/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "yacy_start_crawl",
    "arguments": {
      "url": "https://example.com",
      "depth": 2
    }
  }'
```

## 数据库

服务使用SQLite数据库存储：

- 搜索历史（search_history表）
- 爬虫历史（crawl_history表）

数据库文件位于 `data/yacy_mcp.db`。

## 项目结构

```
src/
├── main/
│   ├── java/com/yacy/mcp/
│   │   ├── YaCyMcpApplication.java    # 主应用类
│   │   ├── client/
│   │   │   └── YaCyClient.java         # YaCy API客户端
│   │   ├── config/
│   │   │   ├── DatabaseConfig.java     # 数据库配置
│   │   │   ├── McpConfig.java          # MCP配置
│   │   │   └── YaCyConfig.java         # YaCy配置
│   │   ├── controller/
│   │   │   └── McpController.java      # REST控制器
│   │   ├── model/
│   │   │   ├── McpTool.java            # MCP工具模型
│   │   │   ├── McpToolCallRequest.java # 工具调用请求
│   │   │   ├── McpToolCallResponse.java # 工具调用响应
│   │   │   └── McpServerInfo.java      # 服务器信息
│   │   └── service/
│   │       ├── DatabaseService.java    # 数据库服务
│   │       └── McpService.java         # MCP服务
│   └── resources/
│       └── application.yml             # 应用配置
└── test/
    └── java/com/yacy/mcp/              # 测试代码
```

## 开发

### 添加新的YaCy API工具

1. 在 `YaCyClient.java` 中添加新的API方法
2. 在 `McpService.java` 的 `listTools()` 中定义工具
3. 在 `McpService.java` 的 `executeTool()` 中添加执行逻辑

## 许可证

参见 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交问题和拉取请求！
