# YaCy MCP 项目总结

## 项目概述

YaCy MCP 是一个纯Java实现的MCP（Model Context Protocol）服务，提供YaCy搜索引擎的完整API功能，集成了Spring AI Alibaba和AgentScope-Java框架。

## 完整技术栈

### 核心框架
- **Spring Boot 3.2.1** - Web应用框架
- **Spring AI Alibaba 1.1.0.0** - AI Agent框架和DashScope集成
- **AgentScope-Java 1.0.8** - Agent运行时平台
- **MCP Spring WebFlux 0.10.0** - MCP协议支持

### 数据层
- **SQLite 3.44.1** - 嵌入式数据库
- **jOOQ 3.18.7** - 类型安全的SQL查询构建器

### 工具库
- **Apache HttpClient 5.3** - HTTP客户端
- **Jackson** - JSON处理
- **Lombok** - 减少样板代码

## 实现的功能

### 1. MCP工具集（9个YaCy API工具）

| 工具名称 | 功能描述 | 参数 |
|---------|---------|------|
| yacy_search | 搜索YaCy索引 | query*, count, offset |
| yacy_get_status | 获取服务器状态 | 无 |
| yacy_get_network | 获取网络信息 | 无 |
| yacy_start_crawl | 启动网页爬虫 | url*, depth |
| yacy_get_index_info | 获取索引信息 | 无 |
| yacy_get_peers | 获取对等节点 | 无 |
| yacy_get_performance | 获取性能统计 | 无 |
| yacy_get_host_browser | 浏览主机 | path |
| yacy_get_document | 获取文档详情 | urlhash* |

*标记为必需参数

### 2. REST API端点

- `GET /mcp/info` - 服务器信息
- `GET /mcp/tools` - 列出所有工具
- `POST /mcp/tools/execute` - 执行工具调用
- `GET /mcp/health` - 健康检查

### 3. 数据持久化

#### search_history表
```sql
CREATE TABLE search_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    query TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    result_count INTEGER,
    execution_time_ms INTEGER
);
```

#### crawl_history表
```sql
CREATE TABLE crawl_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL,
    depth INTEGER,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TEXT
);
```

### 4. 配置管理

#### 环境变量支持
- `YACY_SERVER_URL` - YaCy服务器地址
- `YACY_USERNAME` - YaCy用户名
- `YACY_PASSWORD` - YaCy密码
- `DASHSCOPE_API_KEY` - DashScope API密钥
- `DASHSCOPE_ENABLED` - 启用DashScope

#### 配置文件
- `application.yml` - 主配置文件
- `application.yml.example` - 示例配置

## 安全改进

### 已修复的安全问题

1. **SQL注入防护**
   - 从字符串拼接改为jOOQ类型安全查询
   - 所有数据库操作使用参数化查询

2. **资源泄漏修复**
   - HTTP客户端正确关闭（使用@PreDestroy）
   - 配置连接超时和socket超时

3. **输入验证**
   - 所有必需参数在执行前验证
   - 处理NumberFormatException
   - 返回有意义的错误消息

4. **敏感信息保护**
   - 密码通过环境变量配置
   - 从配置文件中移除硬编码密码

5. **错误处理改进**
   - 区分IllegalArgumentException和其他异常
   - 记录完整堆栈跟踪用于调试
   - 向用户返回清晰的错误消息

## 代码质量

### 测试覆盖
- ✅ 应用程序上下文加载测试
- ✅ MCP服务工具列表测试
- ✅ 工具描述和schema验证测试

### 静态分析
- ✅ CodeQL扫描通过（0个安全警告）
- ✅ 编译无错误
- ✅ 所有测试通过

### 代码组织
```
src/main/java/com/yacy/mcp/
├── YaCyMcpApplication.java        # 主应用类
├── client/
│   └── YaCyClient.java             # YaCy API客户端
├── config/
│   ├── DatabaseConfig.java         # 数据库配置
│   ├── McpConfig.java              # MCP配置
│   └── YaCyConfig.java             # YaCy配置
├── controller/
│   └── McpController.java          # REST控制器
├── model/
│   ├── McpServerInfo.java          # 服务器信息
│   ├── McpTool.java                # 工具定义
│   ├── McpToolCallRequest.java     # 请求模型
│   └── McpToolCallResponse.java    # 响应模型
└── service/
    ├── DatabaseService.java        # 数据库服务
    └── McpService.java             # MCP核心服务
```

## 文档

### 用户文档
- **README.md** - 项目介绍和快速开始
- **USAGE.md** - 详细使用指南
- **AGENTSCOPE_INTEGRATION.md** - AgentScope集成指南

### 配置文档
- **application.yml.example** - 配置示例
- **Dockerfile** - Docker镜像构建
- **docker-compose.yml** - Docker Compose配置

## 部署选项

### 1. 直接运行JAR
```bash
java -jar target/yacy-mcp-1.0.0-SNAPSHOT.jar
```

### 2. Maven运行
```bash
mvn spring-boot:run
```

### 3. Docker部署
```bash
docker build -t yacy-mcp .
docker run -p 8990:8990 yacy-mcp
```

### 4. Docker Compose
```bash
docker-compose up
```

## 性能特性

- **连接池管理**: HTTP客户端配置超时
- **数据库优化**: 使用jOOQ的高效查询
- **异步支持**: Spring Boot异步处理能力
- **资源清理**: 正确的生命周期管理

## 兼容性

- **Java版本**: Java 17+
- **Spring Boot版本**: 3.2.1
- **数据库**: SQLite 3.44.1+
- **YaCy版本**: 任何支持JSON API的版本

## 开发者信息

### 构建命令
```bash
# 完整构建
mvn clean package

# 跳过测试
mvn clean package -DskipTests

# 运行测试
mvn test

# 代码格式化
mvn formatter:format
```

### 环境要求
- Java 17 JDK
- Maven 3.6+
- 可选：Docker（用于容器化部署）

## 已知限制

1. **DashScope需要API密钥**: Spring AI Alibaba功能需要有效的DashScope API密钥
2. **YaCy依赖**: 完整功能需要运行的YaCy实例
3. **单线程数据库**: SQLite适合中小规模使用

## 未来改进建议

1. 添加更多YaCy API端点
2. 实现缓存机制提高性能
3. 添加指标和监控（Micrometer）
4. 支持多种AI模型（不仅仅是DashScope）
5. 增加更多集成测试
6. 添加API文档（Swagger/OpenAPI）

## 许可证

Apache License 2.0

## 贡献者

- GitHub: https://github.com/linkerlin/YaCyMCP
- Issues: https://github.com/linkerlin/YaCyMCP/issues
