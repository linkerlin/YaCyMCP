package com.yacy.mcp.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Database configuration for SQLite
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:jdbc:sqlite:yacy-mcp.db}")
    private String databaseUrl;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl(databaseUrl);
        return dataSource;
    }

    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(new DataSourceConnectionProvider(dataSource));
        configuration.set(SQLDialect.SQLITE);
        return DSL.using(configuration);
    }
}
