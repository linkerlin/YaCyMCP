package com.yacy.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * YaCy MCP Tools Configuration
 * Tools are automatically registered via @Tool annotation
 */
@Configuration
public class YaCyToolsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(YaCyToolsConfiguration.class);

    // Tools are automatically discovered and registered by Spring AI
    // through the @Tool annotation on each tool class
}
