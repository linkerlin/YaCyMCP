package com.yacy.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Paths;

@Configuration
@Profile("!test")
public class DatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

    private static final String DATABASE_DIR = ".yacy-mcp";
    private static final String DATABASE_FILE = "yacy_mcp.db";

    private File databaseDirectory;

    @PostConstruct
    public void init() {
        String userHome = System.getProperty("user.home");
        databaseDirectory = Paths.get(userHome, DATABASE_DIR).toFile();

        if (!databaseDirectory.exists()) {
            if (databaseDirectory.mkdirs()) {
                log.info("Created database directory: {}", databaseDirectory.getAbsolutePath());
            } else {
                log.error("Failed to create database directory: {}", databaseDirectory.getAbsolutePath());
            }
        } else {
            log.debug("Database directory already exists: {}", databaseDirectory.getAbsolutePath());
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public javax.sql.DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + new File(databaseDirectory, DATABASE_FILE).getAbsolutePath());
        log.info("Database URL: jdbc:sqlite:{}", new File(databaseDirectory, DATABASE_FILE).getAbsolutePath());
        return dataSource;
    }
}
