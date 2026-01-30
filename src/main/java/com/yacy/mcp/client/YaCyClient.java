package com.yacy.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yacy.mcp.config.YaCyConfig;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Client for interacting with YaCy API
 */
@Component
public class YaCyClient {

    private static final Logger log = LoggerFactory.getLogger(YaCyClient.class);

    private final YaCyConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final RequestConfig requestConfig;

    public YaCyClient(YaCyConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        // Configure timeouts
        this.requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getConnectionTimeout()))
            .setResponseTimeout(Timeout.ofMilliseconds(config.getSocketTimeout()))
            .build();

        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (httpClient != null) {
                httpClient.close();
                log.info("HTTP client closed successfully");
            }
        } catch (IOException e) {
            log.error("Error closing HTTP client", e);
        }
    }

    /**
     * Perform a search query on YaCy
     */
    public JsonNode search(String query, int count, int offset) throws IOException {
        String url = String.format("%s/yacysearch.json?query=%s&maximumRecords=%d&startRecord=%d",
                config.getServerUrl(),
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                count,
                offset);

        return executeGet(url);
    }

    /**
     * Get YaCy status information
     */
    public JsonNode getStatus() throws IOException {
        String url = config.getServerUrl() + "/Status.json";
        return executeGet(url);
    }

    /**
     * Get network information
     */
    public JsonNode getNetworkInfo() throws IOException {
        String url = config.getServerUrl() + "/Network.json";
        return executeGet(url);
    }

    /**
     * Get crawl start information
     */
    public JsonNode getCrawlStart() throws IOException {
        String url = config.getServerUrl() + "/CrawlStartExpert.json";
        return executeGet(url);
    }

    /**
     * Start crawling a URL
     */
    public JsonNode startCrawl(String crawlUrl, int depth) throws IOException {
        String url = String.format("%s/CrawlStartExpert.json?crawlingMode=url&crawlingURL=%s&crawlingDepth=%d",
                config.getServerUrl(),
                URLEncoder.encode(crawlUrl, StandardCharsets.UTF_8),
                depth);

        return executeGet(url);
    }

    /**
     * Get index information
     */
    public JsonNode getIndexInfo() throws IOException {
        String url = config.getServerUrl() + "/IndexBrowser_p.json";
        return executeGet(url);
    }

    /**
     * Get peer information
     */
    public JsonNode getPeers() throws IOException {
        String url = config.getServerUrl() + "/Network.xml?table=peers";
        return executeGet(url);
    }

    /**
     * Get performance statistics
     */
    public JsonNode getPerformance() throws IOException {
        String url = config.getServerUrl() + "/PerformanceMemory.json";
        return executeGet(url);
    }

    /**
     * Browse hosts in the index
     */
    public JsonNode getHostBrowser(String host, int count) throws IOException {
        String url = String.format("%s/HostBrowser.json?host=%s&count=%d",
                config.getServerUrl(),
                URLEncoder.encode(host, StandardCharsets.UTF_8),
                count);

        return executeGet(url);
    }

    /**
     * Get document details
     */
    public JsonNode getDocument(String url) throws IOException {
        String apiUrl = String.format("%s/yacysearch.json?query=url:%s&maximumRecords=1",
                config.getServerUrl(),
                URLEncoder.encode(url, StandardCharsets.UTF_8));

        return executeGet(apiUrl);
    }

    private JsonNode executeGet(String url) throws IOException {
        HttpGet request = new HttpGet(url);

        // Add authentication if configured
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            String auth = config.getUsername() + ":" + config.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encodedAuth);
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            return objectMapper.readTree(responseBody);
        } catch (ParseException e) {
            throw new IOException("Error parsing response", e);
        }
    }

    private JsonNode executePost(String url, String jsonBody) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        // Add authentication if configured
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            String auth = config.getUsername() + ":" + config.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encodedAuth);
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            return objectMapper.readTree(responseBody);
        } catch (ParseException e) {
            throw new IOException("Error parsing response", e);
        }
    }
}
