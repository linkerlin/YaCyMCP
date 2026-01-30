package com.yacy.mcp.service;

import com.yacy.mcp.model.McpTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpServiceTest {

    @Autowired
    private McpService mcpService;

    @Test
    void testListTools() {
        List<McpTool> tools = mcpService.listTools();
        assertNotNull(tools);
        assertFalse(tools.isEmpty());
        
        // Verify some expected tools are present
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("yacy_search")));
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("yacy_get_status")));
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("yacy_start_crawl")));
    }

    @Test
    void testToolsHaveDescriptions() {
        List<McpTool> tools = mcpService.listTools();
        for (McpTool tool : tools) {
            assertNotNull(tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getInputSchema());
        }
    }
}
