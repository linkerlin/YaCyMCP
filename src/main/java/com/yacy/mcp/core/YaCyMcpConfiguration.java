package com.yacy.mcp.core;

import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * YaCy MCP 配置类
 * 整合 Spring AI Alibaba 和 AgentScope-Java 的配置
 */
@Configuration
public class YaCyMcpConfiguration {

    private static final Logger log = LoggerFactory.getLogger(YaCyMcpConfiguration.class);

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    /**
     * 创建 YaCy MCP 服务器核心
     */
    @Bean
    public YaCyMcpServer yaCyMcpServer(YaCyClient yaCyClient,
                                       DatabaseService databaseService,
                                       Optional<ChatModel> chatModel) {
        return new YaCyMcpServer(yaCyClient, databaseService, chatModel);
    }

    /**
     * 创建 AgentScope 与 Spring AI 的集成服务
     */
    @Bean
    public AgentScopeIntegrationService agentScopeIntegrationService(
            Optional<ChatModel> chatModel,
            YaCyMcpServer mcpServer) {
        return new AgentScopeIntegrationService(chatModel, mcpServer);
    }
}
