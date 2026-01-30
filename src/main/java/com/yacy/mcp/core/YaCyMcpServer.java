package com.yacy.mcp.core;

import com.yacy.mcp.client.YaCyClient;
import com.yacy.mcp.service.DatabaseService;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.message.Msg;
import io.agentscope.core.pipeline.SequentialPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * YaCy MCP 服务器核心类
 * 基于 Spring AI Alibaba 作为 MCP 基座
 * 复用 AgentScope-Java 的 Agent 和 Pipeline 编排能力
 */
@Component
public class YaCyMcpServer {

    private static final Logger log = LoggerFactory.getLogger(YaCyMcpServer.class);

    private final YaCyClient yaCyClient;
    private final DatabaseService databaseService;
    private final Optional<ChatModel> chatModel;
    private final List<Agent> agents = new ArrayList<>();

    public YaCyMcpServer(YaCyClient yaCyClient,
                         DatabaseService databaseService,
                         Optional<ChatModel> chatModel) {
        this.yaCyClient = yaCyClient;
        this.databaseService = databaseService;
        this.chatModel = chatModel;

        log.info("YaCy MCP Server initialized with Spring AI Alibaba");
        if (chatModel.isPresent()) {
            log.info("AI capabilities enabled via Spring AI Alibaba");
        }
    }

    /**
     * 注册 AgentScope Agent
     * 复用 AgentScope 的 Agent 基础架构
     */
    public void registerAgent(Agent agent) {
        agents.add(agent);
        log.info("Registered AgentScope Agent: {}", agent.getName());
    }

    /**
     * 执行 Pipeline 编排
     * 复用 AgentScope 的 Pipeline 功能
     */
    public Mono<Msg> executePipeline(String input, String... agentNames) {
        List<AgentBase> pipelineAgents = new ArrayList<>();
        
        for (String agentName : agentNames) {
            agents.stream()
                    .filter(a -> a.getName().equals(agentName))
                    .filter(a -> a instanceof AgentBase)
                    .map(a -> (AgentBase) a)
                    .findFirst()
                    .ifPresent(pipelineAgents::add);
        }
        
        if (pipelineAgents.isEmpty()) {
            return Mono.just(Msg.builder().textContent("No valid agents found for pipeline").build());
        }
        
        log.info("Executing pipeline with {} agents", pipelineAgents.size());
        
        // 复用 AgentScope 的 SequentialPipeline
        SequentialPipeline pipeline = SequentialPipeline.builder()
                .addAgents(pipelineAgents)
                .build();
        return pipeline.execute(Msg.builder().textContent(input).build());
    }

    /**
     * 执行智能搜索 - 结合 AI 和 YaCy
     * 复用 AgentScope 的 Agent 能力 + Spring AI 的 LLM
     */
    public String intelligentSearch(String query) {
        log.info("Performing intelligent search for: {}", query);
        
        try {
            // 首先执行 YaCy 搜索
            long startTime = System.currentTimeMillis();
            var searchResults = yaCyClient.search(query, 10, 0);
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录搜索历史
            databaseService.logSearch(query, 
                    searchResults.has("channels") ? searchResults.get("channels").size() : 0, 
                    duration);
            
            // 如果启用了 AI，使用 AI 总结结果
            if (chatModel.isPresent()) {
                return summarizeWithAi(query, searchResults.toString());
            }
            
            return searchResults.toString();
            
        } catch (Exception e) {
            log.error("Error in intelligent search", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 使用 Spring AI 总结搜索结果
     */
    private String summarizeWithAi(String query, String searchResults) {
        try {
            // 限制结果长度
            if (searchResults.length() > 8000) {
                searchResults = searchResults.substring(0, 8000) + "...";
            }
            
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage("You are a helpful assistant analyzing YaCy search results."),
                    new UserMessage(String.format("""
                            User Query: %s
                            
                            Search Results (JSON):
                            %s
                            
                            Please provide a concise summary of the search results.
                            Highlight the most relevant information found.
                            """, query, searchResults))
            ));
            
            return chatModel.get().call(prompt).getResult().getOutput().getText();
            
        } catch (Exception e) {
            log.error("Error summarizing with AI", e);
            return "Raw results: " + searchResults;
        }
    }

    /**
     * 异步执行 Agent 任务
     * 复用 AgentScope 的异步能力
     */
    public CompletableFuture<Msg> executeAsync(String agentName, String input) {
        Optional<Agent> agent = agents.stream()
                .filter(a -> a.getName().equals(agentName))
                .findFirst();
        
        if (agent.isEmpty()) {
            return CompletableFuture.completedFuture(
                    Msg.builder().textContent("Agent not found: " + agentName).build());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("Async executing agent: {}", agentName);
            return agent.get().call(Msg.builder().textContent(input).build()).block();
        });
    }

    /**
     * 获取服务器状态
     */
    public Map<String, Object> getStatus() {
        return Map.of(
                "name", "YaCy MCP Server",
                "version", "1.0.0",
                "framework", "Spring AI Alibaba + AgentScope-Java",
                "agents_count", agents.size(),
                "ai_enabled", chatModel.isPresent(),
                "yacy_connected", checkYaCyConnection()
        );
    }

    private boolean checkYaCyConnection() {
        try {
            yaCyClient.getStatus();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
