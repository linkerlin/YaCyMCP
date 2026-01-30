# YaCy MCP 使用指南

## 概述

YaCy MCP 是一个纯Java实现的MCP（Model Context Protocol）服务，提供了YaCy搜索引擎的完整API功能。本服务采用Spring Boot框架，使用SQLite数据库存储历史记录，通过jOOQ进行数据库操作。

## 技术架构

### 核心技术栈
- **Spring Boot 3.2.1** - 应用框架
- **SQLite 3.44.1** - 轻量级嵌入式数据库
- **jOOQ 3.18.7** - 类型安全的SQL构建器
- **Apache HttpClient 5** - HTTP客户端
- **Jackson** - JSON处理
- **Lombok** - 减少样板代码

### 项目结构

```
com.yacy.mcp/
├── YaCyMcpApplication.java      # Spring Boot主应用类
├── client/
│   └── YaCyClient.java           # YaCy API客户端封装
├── config/
│   ├── DatabaseConfig.java       # 数据库配置
│   ├── McpConfig.java            # MCP服务配置
│   └── YaCyConfig.java           # YaCy连接配置
├── controller/
│   └── McpController.java        # REST API控制器
├── model/
│   ├── McpServerInfo.java        # 服务器信息模型
│   ├── McpTool.java              # 工具定义模型
│   ├── McpToolCallRequest.java   # 工具调用请求
│   └── McpToolCallResponse.java  # 工具调用响应
└── service/
    ├── DatabaseService.java      # 数据库服务
    └── McpService.java           # MCP核心服务
```

## 安装和配置

### 前置条件

1. **Java 17或更高版本**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **YaCy服务器** (可选)
   - 如果要使用完整功能，需要运行YaCy服务器
   - 下载地址: https://yacy.net/
   - 默认运行在 http://localhost:8090

### 构建项目

```bash
# 克隆仓库
git clone https://github.com/linkerlin/YaCyMCP.git
cd YaCyMCP

# 编译项目
mvn clean package

# 运行测试
mvn test
```

### 配置文件

编辑 `src/main/resources/application.yml`:

```yaml
# YaCy服务器配置
yacy:
  server-url: http://localhost:8090    # YaCy服务器地址
  username: admin                       # YaCy管理员用户名
  password: ""                          # YaCy管理员密码
  connection-timeout: 30000             # 连接超时(毫秒)
  socket-timeout: 30000                 # Socket超时(毫秒)

# MCP服务配置
mcp:
  server-name: YaCy MCP Service
  server-version: 1.0.0
  server-port: 8080
  agent-scope-enabled: true

# 日志配置
logging:
  level:
    com.yacy.mcp: INFO
```

## 运行服务

### 方式1: 使用Maven

```bash
mvn spring-boot:run
```

### 方式2: 运行JAR包

```bash
java -jar target/yacy-mcp-1.0.0-SNAPSHOT.jar
```

### 方式3: 后台运行

```bash
nohup java -jar target/yacy-mcp-1.0.0-SNAPSHOT.jar > app.log 2>&1 &
```

服务启动后，默认监听端口 `8080`。

## API使用

### 1. 健康检查

```bash
curl http://localhost:8080/mcp/health
```

响应:
```
OK
```

### 2. 获取服务器信息

```bash
curl http://localhost:8080/mcp/info
```

响应:
```json
{
  "name": "YaCy MCP Service",
  "version": "1.0.0",
  "capabilities": ["tools", "yacy-api"]
}
```

### 3. 列出所有可用工具

```bash
curl http://localhost:8080/mcp/tools
```

### 4. 执行工具调用

#### 搜索示例

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

#### 获取状态示例

```bash
curl -X POST http://localhost:8080/mcp/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "yacy_get_status",
    "arguments": {}
  }'
```

#### 启动爬虫示例

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

## 可用工具列表

| 工具名称 | 描述 | 必需参数 | 可选参数 |
|---------|------|----------|----------|
| `yacy_search` | 搜索YaCy索引 | query | count, offset |
| `yacy_get_status` | 获取服务器状态 | - | - |
| `yacy_get_network` | 获取网络信息 | - | - |
| `yacy_start_crawl` | 启动网页爬虫 | url | depth |
| `yacy_get_index_info` | 获取索引信息 | - | - |
| `yacy_get_peers` | 获取对等节点 | - | - |
| `yacy_get_performance` | 获取性能统计 | - | - |
| `yacy_get_host_browser` | 浏览主机 | - | path |
| `yacy_get_document` | 获取文档详情 | urlhash | - |

## 数据库

服务使用SQLite数据库存储数据，位于 `data/yacy_mcp.db`。

### 表结构

#### search_history - 搜索历史
```sql
CREATE TABLE search_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    query TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    result_count INTEGER,
    execution_time_ms INTEGER
);
```

#### crawl_history - 爬虫历史
```sql
CREATE TABLE crawl_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL,
    depth INTEGER,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TEXT
);
```

### 查询数据库

```bash
# 查看搜索历史
sqlite3 data/yacy_mcp.db "SELECT * FROM search_history;"

# 查看爬虫历史
sqlite3 data/yacy_mcp.db "SELECT * FROM crawl_history;"
```

## 开发指南

### 添加新的YaCy API工具

1. **在YaCyClient中添加API方法**:

```java
public JsonNode getNewFeature() throws IOException {
    String url = config.getServerUrl() + "/NewFeature.json";
    return executeGet(url);
}
```

2. **在McpService的listTools()中注册工具**:

```java
tools.add(new McpTool(
    "yacy_new_feature",
    "Description of new feature",
    createSchema(Map.of(), List.of())
));
```

3. **在McpService的executeTool()中添加执行逻辑**:

```java
case "yacy_new_feature" -> yaCyClient.getNewFeature();
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=McpServiceTest
```

## 故障排查

### 问题1: 无法连接到YaCy服务器

**解决方案**:
- 确认YaCy服务器正在运行
- 检查`application.yml`中的`yacy.server-url`配置
- 验证网络连接和防火墙设置

### 问题2: 数据库错误

**解决方案**:
- 检查`data`目录权限
- 删除`data/yacy_mcp.db`重新启动服务
- 查看日志文件中的详细错误信息

### 问题3: 端口已被占用

**解决方案**:
修改`application.yml`中的端口:
```yaml
server:
  port: 8081  # 使用其他端口
```

## 性能优化

1. **调整连接超时**:
   ```yaml
   yacy:
     connection-timeout: 60000  # 增加超时时间
   ```

2. **数据库优化**:
   - 定期清理历史记录
   - 添加适当的索引

3. **日志级别**:
   ```yaml
   logging:
     level:
       com.yacy.mcp: WARN  # 减少日志输出
   ```

## 安全建议

1. **使用认证**: 在生产环境中启用YaCy的身份验证
2. **HTTPS**: 使用HTTPS保护通信
3. **防火墙**: 限制对MCP服务的访问
4. **定期更新**: 保持依赖库的最新版本

## 许可证

本项目采用 Apache License 2.0 许可证。详见 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交问题报告和拉取请求！

## 联系方式

- GitHub: https://github.com/linkerlin/YaCyMCP
- Issues: https://github.com/linkerlin/YaCyMCP/issues
