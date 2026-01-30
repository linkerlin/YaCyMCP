package com.yacy.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * YaCy MCP Service Application
 *
 * A pure Java MCP service providing YaCy search engine API functionalities.
 *
 * Technology Stack:
 * - Spring Boot 3.2.1 (Web framework)
 * - Spring AI Alibaba 1.1.0.0 (MCP + AI integration)
 * - AgentScope-Java 1.0.8 (Agent runtime platform)
 * - SQLite + jOOQ (Database)
 * - MCP Java SDK 0.8.0 (Official MCP protocol support)
 *
 * MCP Tools Provided:
 * - yacy_search: Search the YaCy index
 * - yacy_get_status: Get server status
 * - yacy_get_network: Get network information
 * - yacy_start_crawl: Start web crawling
 * - yacy_get_index_info: Get index information
 * - yacy_get_peers: Get peer information
 * - yacy_get_performance: Get performance stats
 * - yacy_get_host_browser: Browse hosts
 * - yacy_get_document: Get document details
 *
 * Features:
 * - Spring AI Alibaba as MCP base implementation
 * - Reuse AgentScope-Java components (Agent, Pipeline)
 * - Intelligent search with AI enhancement
 * - Async Agent execution
 */
@SpringBootApplication
public class YaCyMcpApplication {

    private static final Logger log = LoggerFactory.getLogger(YaCyMcpApplication.class);

    public static void main(String[] args) {
        log.info("Starting YaCy MCP Service with Spring AI Alibaba + AgentScope-Java...");
        SpringApplication.run(YaCyMcpApplication.class, args);
        log.info("YaCy MCP Service started successfully");
    }
}
