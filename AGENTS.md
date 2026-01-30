所有的日志都输出 stderr ，不输出到 stdout。以免干扰MCP的正常运行。

---
默认管理员账号
用户名/密码：
admin/steper123456789

---
## BUG 修复经验总结

### 问题描述

**McpStdioOutputTest 测试卡住**

运行 `mvn test -Dtest="McpStdioOutputTest"` 时测试长时间无响应，虽然最终能完成，但存在以下问题：

1. 使用反射调用私有方法 `handleMessage`，存在线程竞争风险
2. `sendRequestToServer` 方法使用 `ExecutorService` + `Future.get(3秒)` 超时机制不可靠
3. 每个测试用例创建新的服务器实例，资源管理混乱
4. 没有提前验证 YaCy 服务器可用性，导致不必要的等待

### 根本原因分析

#### 原因 1: 反射调用与线程竞争

```java
// 原来的代码 - 问题代码
private void sendRequestToServer(McpStdioServer server, String input) throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> future = executor.submit(() -> {
        // 使用反射调用私有方法
        java.lang.reflect.Method method = McpStdioServer.class.getDeclaredMethod("handleMessage", String.class);
        method.setAccessible(true);
        method.invoke(server, line);
    });
    // 超时机制不可靠
    future.get(3, TimeUnit.SECONDS);
}
```

问题：
- 反射调用私有方法打破封装
- 多线程环境下可能有竞态条件
- `Future.get()` 超时后 cancel 可能不生效

#### 原因 2: 资源管理混乱

每个测试都重复：
1. 创建 `McpStdioServer` 实例
2. 调用 `start()` 启动
3. 等待 500ms
4. 调用 `stop()` 停止

没有统一的资源管理，容易造成线程泄漏。

#### 原因 3: 缺少前置检查

在 `setUp()` 中通过 `yaCyClient.search()` 检查可用性，但如果 YaCy 不可用，会导致后续所有工具调用失败。

### 解决方案

#### 修复 1: 使用公共 API `processRequest()`

McpStdioServer 已有公共方法 `processRequest()`，直接使用：

```java
// 修复后的代码
runTestWithServer(server -> {
    String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"test-1\",\"method\":\"initialize\",...}\n";
    server.processRequest(initRequest);  // 直接调用，无需反射
});
```

#### 修复 2: 封装统一的测试辅助方法

```java
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
```

好处：
- 统一管理服务器生命周期
- `finally` 块确保资源释放
- 测试代码更简洁

#### 修复 3: 使用 `assumeTrue` 跳过不可用测试

```java
@BeforeAll
static void checkYaCyAvailability() {
    yacyAvailable = checkYaCyApiAvailable(YACY_API_URL, 5);
}

@Test
@Order(10)
@DisplayName("测试 initialize 响应")
void testInitializeResponse() throws Exception {
    assumeTrue(yacyAvailable, "YaCy 服务器不可用，跳过测试");
    // 测试逻辑
}
```

好处：
- YaCy 不可用时优雅跳过
- 快速失败，不用等待超时
- 明确标记跳过的原因

### 关键经验教训

1. **优先使用公共 API**
   - 反射调用私有方法是最后手段
   - 公共方法经过完整测试，更可靠

2. **统一资源管理**
   - 使用辅助方法封装资源生命周期
   - `finally` 块确保资源释放

3. **快速失败原则**
   - 前置条件检查，避免无效等待
   - 使用 `assumeTrue`/`assumeFalse` 跳过不适用测试

4. **测试隔离**
   - 每个测试使用独立的服务器实例
   - 但共享通用的辅助方法

5. **日志与输出分离**
   - 使用 `setOutputStream()` 重定向输出
   - 避免日志干扰 stdout 的协议输出

### 相关文件修改

- `McpStdioOutputTest.java` - 重构测试类，移除反射调用
- `McpService.java` - 修复 `databaseService` null 检查

---

## Windows 平台 MCP 服务连接卡住问题

### 问题描述

**现象**: 在 macOS 上调试 MCP 服务正常，但切换到 Windows 机器上 Kimi CLI 连接时卡住。

**环境**:
- Windows 25H2
- 多个 Java 版本共存（JDK 25、JDK 17、SDKMAN 管理的 Java）
- YaCy 服务运行正常（localhost:8090 可访问）
- JAR 文件存在且可执行

**错误表现**:
- Kimi CLI 报告 "MCP 服务器连接失败"
- 服务看似启动但无响应
- 手动测试 `echo '{...}' | java -jar yacy-mcp.jar` 时出现类加载错误

### 根本原因分析

#### 原因 1: Windows PowerShell 参数解析问题

```powershell
# 错误：PowerShell 将 -Dfile.encoding=UTF-8 解析为类名
java -Dfile.encoding=UTF-8 -jar yacy-mcp.jar
# 报错: 找不到或无法加载主类 .encoding=UTF-8

# 正确：参数需要加引号
java "-Dfile.encoding=UTF-8" -jar yacy-mcp.jar
```

#### 原因 2: MCP 配置中 Java 路径不明确

原配置只使用 `java` 命令，依赖 PATH 环境变量：

```json
{
  "command": "java",
  "args": ["-Dfile.encoding=UTF-8", "-jar", "..."]
}
```

问题：
- Windows 上有多个 Java 版本，`java` 可能解析到不兼容的版本
- MCP 客户端启动进程的方式可能与 shell 不同，参数传递方式有差异

#### 原因 3: 编码参数不完整

Windows 默认使用 GBK 编码，而 MCP 协议使用 UTF-8，需要多个编码参数确保兼容。

### 解决方案

#### 修复后的 MCP 配置

```json
{
  "mcpServers": {
    "yacy-mcp": {
      "command": "C:/Program Files/Java/jdk-17/bin/java.exe",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-Dstdout.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
        "-jar",
        "C:/GitHub/YaCyMCP/yacy-mcp.jar"
      ],
      "env": {
        "YACY_API_URL": "http://localhost:8090",
        "YACY_USERNAME": "admin",
        "YACY_PASSWORD": "steper123456789",
        "DASHSCOPE_API_KEY": "your-dashscope-api-key",
        "DASHSCOPE_ENABLED": "false",
        "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8"
      },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

#### 关键修复点

1. **使用 Java 完整路径**
   - `"command": "C:/Program Files/Java/jdk-17/bin/java.exe"`
   - 避免依赖 PATH 环境变量
   - 确保使用兼容的 Java 版本（17+）

2. **完整的编码参数**
   - `-Dfile.encoding=UTF-8` - 文件编码
   - `-Dstdout.encoding=UTF-8` - 标准输出编码
   - `-Dsun.stdout.encoding=UTF-8` - Sun JVM 标准输出编码
   - `-Dsun.stderr.encoding=UTF-8` - Sun JVM 标准错误编码

3. **环境变量双重保障**
   - `JAVA_TOOL_OPTIONS: "-Dfile.encoding=UTF-8"` 确保 JVM 启动时使用 UTF-8

### 诊断方法

#### 验证服务是否正常

```powershell
# 测试 MCP 服务响应
'{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}' | java "-Dfile.encoding=UTF-8" -jar yacy-mcp.jar 2>$null

# 预期输出：包含 protocolVersion 和 serverInfo 的 JSON 响应
```

#### 检查 Java 版本和路径

```powershell
# 查看所有 Java 安装位置
where.exe java

# 检查默认 Java 版本
java -version
```

#### 验证 YaCy 服务

```powershell
# 测试 YaCy 端口连通性
Test-NetConnection -ComputerName localhost -Port 8090 -InformationLevel Quiet
```

### 关键经验教训

1. **跨平台兼容性**
   - Windows 和 macOS/Linux 的 shell 参数解析方式不同
   - 始终使用完整路径避免环境差异

2. **多 Java 版本环境**
   - 明确指定 Java 可执行文件的完整路径
   - 避免依赖系统 PATH 变量

3. **编码问题**
   - Windows 默认编码（GBK）与 UTF-8 不兼容
   - MCP 协议使用 JSON，必须确保 UTF-8 编码
   - 使用多个编码参数双重保障

4. **MCP stdio 模式调试**
   - 可以用管道手动测试 JSON-RPC 请求
   - 检查 stderr 日志（`yacy-mcp.log`）排查问题
   - 确保没有任何输出到 stdout 干扰协议通信

### 相关文件

- `~/.kimi/mcp.json` - Kimi CLI 的 MCP 配置文件
- `start-mcp.cmd` - Windows 启动脚本（备用方案）
- `yacy-mcp.log` - 服务日志文件
