package com.yacy.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yacy.mcp.config.YaCyConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Client for interacting with YaCy API
 */
@Slf4j
@Component
public class YaCyClient {

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
     * Start a new crawl
     */
    public JsonNode startCrawl(String crawlingURL, int crawlingDepth) throws IOException {
        String url = config.getServerUrl() + "/CrawlStartExpert.json";
        
        HttpPost post = new HttpPost(url);
        addAuthHeader(post);
        
        String body = String.format("crawlingURL=%s&crawlingDepth=%d&crawlingMode=url",
                URLEncoder.encode(crawlingURL, StandardCharsets.UTF_8),
                crawlingDepth);
        
        post.setEntity(new StringEntity(body));
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        
        return executeRequest(post);
    }

    /**
     * Get index information
     */
    public JsonNode getIndexInfo() throws IOException {
        String url = config.getServerUrl() + "/IndexControlRWIs_p.json";
        return executeGet(url);
    }

    /**
     * Get peers information
     */
    public JsonNode getPeers() throws IOException {
        String url = config.getServerUrl() + "/Peers_p.json";
        return executeGet(url);
    }

    /**
     * Get performance statistics
     */
    public JsonNode getPerformance() throws IOException {
        String url = config.getServerUrl() + "/PerformanceQueues_p.json";
        return executeGet(url);
    }

    /**
     * Get host browser information
     */
    public JsonNode getHostBrowser(String path) throws IOException {
        String url = String.format("%s/HostBrowser.json?path=%s",
                config.getServerUrl(),
                URLEncoder.encode(path != null ? path : "", StandardCharsets.UTF_8));
        return executeGet(url);
    }

    /**
     * Get search result details
     */
    public JsonNode getSearchResult(String urlhash) throws IOException {
        String url = String.format("%s/yacydoc.json?urlhash=%s",
                config.getServerUrl(),
                URLEncoder.encode(urlhash, StandardCharsets.UTF_8));
        return executeGet(url);
    }

    private JsonNode executeGet(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setConfig(requestConfig);
        addAuthHeader(get);
        return executeRequest(get);
    }

    private JsonNode executeRequest(org.apache.hc.core5.http.ClassicHttpRequest request) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            log.debug("YaCy API response: {}", responseBody);
            return objectMapper.readTree(responseBody);
        } catch (org.apache.hc.core5.http.ParseException e) {
            throw new IOException("Failed to parse response", e);
        }
    }

    private void addAuthHeader(org.apache.hc.core5.http.ClassicHttpRequest request) {
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            String auth = config.getUsername() + ":" + config.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encodedAuth);
        }
    }
}
