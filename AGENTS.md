所有的日志都输出 stderr ，不输出到 stdout。以免干扰MCP的正常运行。

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
