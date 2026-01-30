package com.yacy.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@Profile("test")
public class TestDatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public javax.sql.DataSource dataSource() {
        String userHome = System.getProperty("user.home");
        java.io.File testDbDir = java.nio.file.Paths.get(userHome, ".yacy-mcp").toFile();
        if (!testDbDir.exists()) {
            testDbDir.mkdirs();
        }
        java.io.File testDbFile = new java.io.File(testDbDir, "yacy_mcp_test.db");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + testDbFile.getAbsolutePath());
        log.info("Test Database URL: jdbc:sqlite:{}", testDbFile.getAbsolutePath());
        return dataSource;
    }
}
