package com.yacy.mcp.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for YaCy connection
 * Supports environment variable YACY_API_URL for server URL
 */
@Configuration
@ConfigurationProperties(prefix = "yacy")
public class YaCyConfig {

    private static final Logger log = LoggerFactory.getLogger(YaCyConfig.class);

    /**
     * Environment variable name for YaCy API URL
     */
    private static final String ENV_YACY_API_URL = "YACY_API_URL";

    /**
     * Environment variable name for YaCy server URL (fallback)
     */
    private static final String ENV_YACY_SERVER_URL = "YACY_SERVER_URL";

    /**
     * Default YaCy server URL
     */
    private static final String DEFAULT_SERVER_URL = "http://localhost:8090";

    /**
     * YaCy server URL
     */
    private String serverUrl = DEFAULT_SERVER_URL;

    /**
     * YaCy admin username
     */
    private String username = "admin";

    /**
     * YaCy admin password
     */
    private String password = "";

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 30000;

    /**
     * Socket timeout in milliseconds
     */
    private int socketTimeout = 30000;

    /**
     * Initialize configuration - check environment variables
     */
    @PostConstruct
    public void init() {
        // Priority: YACY_API_URL > YACY_SERVER_URL > application.yml > default
        String envApiUrl = System.getenv(ENV_YACY_API_URL);
        String envServerUrl = System.getenv(ENV_YACY_SERVER_URL);

        if (envApiUrl != null && !envApiUrl.isEmpty()) {
            this.serverUrl = envApiUrl;
            log.info("Using YaCy server URL from environment variable {}: {}", ENV_YACY_API_URL, serverUrl);
        } else if (envServerUrl != null && !envServerUrl.isEmpty()) {
            this.serverUrl = envServerUrl;
            log.info("Using YaCy server URL from environment variable {}: {}", ENV_YACY_SERVER_URL, serverUrl);
        } else {
            log.info("Using YaCy server URL from configuration: {}", serverUrl);
        }

        // Validate URL
        if (!this.serverUrl.startsWith("http://") && !this.serverUrl.startsWith("https://")) {
            log.warn("YaCy server URL does not start with http:// or https://: {}", serverUrl);
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}
