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

服务将在 http://localhost:8990 启动。

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

## Kimi CLI 配置

在 Kimi CLI 中使用此 MCP 服务，需要在配置文件中添加 MCP 服务器配置。

### 1. 启动 MCP 服务

确保 MCP 服务已启动并运行在 `http://localhost:8990`：

```bash
mvn spring-boot:run
# 或
java -jar target/yacy-mcp-1.0.0-SNAPSHOT.jar
```

### 2. 配置 Kimi CLI

编辑 Kimi CLI 的配置文件（通常位于 `~/.kimi/config.json` 或项目根目录的 `.kimi/config.json`）：

```json
{
  "mcpServers": {
    "yacy-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/yacy-mcp-1.0.0-SNAPSHOT.jar"
      ],
      "env": {
        "YACY_SERVER_URL": "http://localhost:8090",
        "DASHSCOPE_API_KEY": "your-dashscope-api-key",
        "DASHSCOPE_ENABLED": "false"
      },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

**配置说明：**

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `command` | 启动命令 | `java` |
| `args` | 命令参数 | `["-jar", "/path/to/yacy-mcp-1.0.0-SNAPSHOT.jar"]` |
| `env.YACY_SERVER_URL` | YaCy 服务器地址 | `http://localhost:8090` |
| `env.DASHSCOPE_API_KEY` | 阿里云 DashScope API 密钥 | 空 |
| `env.DASHSCOPE_ENABLED` | 是否启用 AI 功能 | `false` |
| `disabled` | 是否禁用此 MCP 服务器 | `false` |
| `autoApprove` | 自动批准的操作列表 | `[]` |

### 3. 使用 MCP 工具

配置完成后，在 Kimi CLI 中可以直接使用以下工具：

```
@yacy_search query="人工智能" count=10
@yacy_get_status
@yacy_start_crawl url="https://example.com" depth=2
```

### 4. 完整配置示例

```json
{
  "mcpServers": {
    "yacy-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/username/projects/YaCyMCP/target/yacy-mcp-1.0.0-SNAPSHOT.jar"
      ],
      "env": {
        "YACY_SERVER_URL": "http://localhost:8090",
        "YACY_USERNAME": "admin",
        "YACY_PASSWORD": "",
        "DASHSCOPE_API_KEY": "sk-xxxxxxxxxxxxxxxx",
        "DASHSCOPE_ENABLED": "true",
        "SERVER_PORT": "8990"
      },
      "disabled": false,
      "autoApprove": ["yacy_search", "yacy_get_status"]
    }
  }
}
```

### 5. 验证配置

在 Kimi CLI 中运行以下命令验证 MCP 服务器是否正常工作：

```bash
@mcp list
```

如果配置正确，将看到 `yacy-mcp` 服务器及其可用工具列表。

## API端点

### 新的MCP标准端点 (推荐)

基于MCP SDK的标准实现:

- `GET /mcp/sse` - SSE连接端点 (Server-Sent Events)
- `POST /mcp/message` - MCP消息发送端点
- `GET /mcp/health` - 健康检查

### Legacy端点 (已弃用)

- `GET /mcp/legacy/info` - 获取服务器信息
- `GET /mcp/legacy/tools` - 列出所有可用工具
- `POST /mcp/legacy/tools/execute` - 执行工具调用
- `GET /mcp/legacy/health` - 健康检查

### 示例请求

#### 连接MCP服务器 (SSE)

```javascript
const eventSource = new EventSource('http://localhost:8990/mcp/sse');

eventSource.onmessage = (event) => {
  console.log('MCP Message:', JSON.parse(event.data));
};
```

#### 调用工具 (通过MCP协议)

```javascript
fetch('http://localhost:8990/mcp/message?sessionId=your-session-id', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    method: 'tools/call',
    params: {
      name: 'yacy_search',
      arguments: {
        query: 'AI research',
        count: 20
      }
    }
  })
});
```

#### Legacy方式执行搜索 (已弃用)

```bash
curl -X POST http://localhost:8990/mcp/legacy/tools/execute \
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
