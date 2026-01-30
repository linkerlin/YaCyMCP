package com.yacy.mcp.service;

import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for managing search history in SQLite database
 */
@Slf4j
@Service
public class DatabaseService {

    private final DSLContext dsl;

    public DatabaseService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @PostConstruct
    public void init() {
        createTables();
    }

    private void createTables() {
        try {
            // Create search_history table
            dsl.execute("""
                CREATE TABLE IF NOT EXISTS search_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    query TEXT NOT NULL,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    result_count INTEGER,
                    execution_time_ms INTEGER
                )
            """);

            // Create crawl_history table
            dsl.execute("""
                CREATE TABLE IF NOT EXISTS crawl_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    url TEXT NOT NULL,
                    depth INTEGER,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status TEXT
                )
            """);

            log.info("Database tables initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing database tables", e);
        }
    }

    public void logSearch(String query, int resultCount, long executionTimeMs) {
        try {
            dsl.insertInto(
                org.jooq.impl.DSL.table("search_history"),
                org.jooq.impl.DSL.field("query"),
                org.jooq.impl.DSL.field("result_count"),
                org.jooq.impl.DSL.field("execution_time_ms")
            ).values(query, resultCount, executionTimeMs).execute();
        } catch (Exception e) {
            log.error("Error logging search", e);
        }
    }

    public void logCrawl(String url, int depth, String status) {
        try {
            dsl.insertInto(
                org.jooq.impl.DSL.table("crawl_history"),
                org.jooq.impl.DSL.field("url"),
                org.jooq.impl.DSL.field("depth"),
                org.jooq.impl.DSL.field("status")
            ).values(url, depth, status).execute();
        } catch (Exception e) {
            log.error("Error logging crawl", e);
        }
    }
}
