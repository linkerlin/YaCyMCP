package com.yacy.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * YaCy MCP Service Application
 * A pure Java MCP service containing YaCy API functionalities
 * Technology Stack: Spring AI Alibaba, SQLite DB, jOOQ, agentscope-java
 */
@SpringBootApplication
public class YaCyMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaCyMcpApplication.class, args);
    }
}
