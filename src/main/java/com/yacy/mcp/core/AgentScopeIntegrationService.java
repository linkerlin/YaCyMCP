package com.yacy.mcp.core;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * AgentScope 与 Spring AI Alibaba 集成服务
 * 桥接两个框架，实现功能复用
 */
@Service
public class AgentScopeIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(AgentScopeIntegrationService.class);

    private final Optional<ChatModel> chatModel;
    private final YaCyMcpServer mcpServer;

    public AgentScopeIntegrationService(Optional<ChatModel> chatModel,
                                        YaCyMcpServer mcpServer) {
        this.chatModel = chatModel;
        this.mcpServer = mcpServer;

        log.info("AgentScopeIntegrationService initialized");
        log.info("Spring AI ChatModel available: {}", chatModel.isPresent());
    }

    /**
     * 使用 AgentScope Agent 处理任务，并可选择使用 Spring AI 增强
     */
    public String processWithAgent(Agent agent, String input, boolean enhanceWithAi) {
        log.info("Processing with Agent {} (enhanceWithAi={})", agent.getName(), enhanceWithAi);

        // 使用 AgentScope Agent 处理
        Msg message = Msg.builder().textContent(input).build();
        Msg result = agent.call(message).block();
        String output = extractText(result);

        // 如果需要，使用 Spring AI 增强结果
        if (enhanceWithAi && chatModel.isPresent()) {
            output = enhanceOutput(input, output);
        }

        return output;
    }

    /**
     * 创建增强型 Agent，结合 AgentScope 和 Spring AI
     */
    public Agent createEnhancedAgent(String name, String systemPrompt) {
        return new Agent() {
            @Override
            public String getAgentId() {
                return "enhanced-agent-" + name;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Mono<Msg> call(Msg input) {
                log.info("Enhanced Agent {} processing", name);

                // 如果 Spring AI 可用，使用它进行更智能的处理
                if (chatModel.isPresent()) {
                    String userInput = extractText(input);
                    String enhancedResult = processWithSpringAi(systemPrompt, userInput);
                    return Mono.just(Msg.builder().textContent(enhancedResult).build());
                }

                // 否则返回原始输入
                return Mono.just(input);
            }

            @Override
            public Mono<Msg> call(List<Msg> msgs) {
                if (msgs.isEmpty()) {
                    return Mono.just(Msg.builder().textContent("No input messages").build());
                }
                return call(msgs.get(msgs.size() - 1));
            }

            @Override
            public Mono<Msg> call(List<Msg> msgs, Class<?> structuredModel) {
                return call(msgs);
            }

            @Override
            public Mono<Msg> call(List<Msg> msgs, com.fasterxml.jackson.databind.JsonNode schema) {
                return call(msgs);
            }

            @Override
            public Flux<Event> stream(List<Msg> msgs, StreamOptions options) {
                return Flux.empty();
            }

            @Override
            public Flux<Event> stream(List<Msg> msgs, StreamOptions options, Class<?> structuredModel) {
                return Flux.empty();
            }

            @Override
            public void interrupt() {
                // No-op
            }

            @Override
            public void interrupt(Msg msg) {
                // No-op
            }

            @Override
            public reactor.core.publisher.Mono<Void> observe(Msg msg) {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Mono<Void> observe(java.util.List<Msg> msgs) {
                return reactor.core.publisher.Mono.empty();
            }
        };
    }

    /**
     * 使用 Spring AI 处理
     */
    private String processWithSpringAi(String systemPrompt, String userInput) {
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userInput)
            ));

            return chatModel.get().call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Error processing with Spring AI", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 使用 Spring AI 增强 Agent 输出
     */
    private String enhanceOutput(String originalInput, String agentOutput) {
        try {
            String promptText = String.format("""
                    Original Input: %s
                    Agent Output: %s

                    Please enhance and format the above output to make it more readable and useful.
                    """, originalInput, agentOutput);

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage("You are a helpful assistant that enhances agent outputs."),
                    new UserMessage(promptText)
            ));

            return chatModel.get().call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Error enhancing output", e);
            return agentOutput;
        }
    }

    /**
     * 从 Msg 中提取文本
     */
    private String extractText(Msg msg) {
        if (msg == null || msg.getContent() == null || msg.getContent().isEmpty()) {
            return "";
        }
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .findFirst()
                .orElse("");
    }

    /**
     * 检查是否启用了 AI 功能
     */
    public boolean isAiEnabled() {
        return chatModel.isPresent();
    }
}
