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
import java.util.List;
import java.util.Map;

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
     * Uses Solr stats API which is publicly accessible
     */
    public JsonNode getStatus() throws IOException {
        // Use Solr stats to get basic status info (publicly accessible)
        String url = config.getServerUrl() + "/solr/select?q=*:*&rows=0&wt=json";
        JsonNode solrResult = executeGet(url);
        
        // Also try to get version info from search API
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("serverUrl", config.getServerUrl());
        status.put("solrStatus", solrResult.path("responseHeader").path("status").asInt());
        status.put("documentsInIndex", solrResult.path("response").path("numFound").asLong());
        status.put("queryTime", solrResult.path("responseHeader").path("QTime").asInt());
        status.put("available", true);
        
        return objectMapper.valueToTree(status);
    }

    /**
     * Get network information
     * Uses seedlist.json to get peer statistics
     */
    public JsonNode getNetworkInfo() throws IOException {
        String url = config.getServerUrl() + "/yacy/seedlist.json";
        JsonNode peersResult = executeGet(url);
        
        // Build network statistics from peers data
        Map<String, Object> networkInfo = new java.util.LinkedHashMap<>();
        JsonNode peers = peersResult.path("peers");
        
        int totalPeers = 0;
        int activePeers = 0;
        long totalLinks = 0;
        long totalWords = 0;
        
        if (peers.isArray()) {
            totalPeers = peers.size();
            for (JsonNode peer : peers) {
                // Count active peers (those seen recently)
                String lastSeen = peer.path("LastSeen").asText("");
                if (!lastSeen.isEmpty()) {
                    activePeers++;
                }
                // Sum up links and words
                totalLinks += peer.path("LCount").asLong(0);
                totalWords += peer.path("ICount").asLong(0);
            }
        }
        
        networkInfo.put("totalPeers", totalPeers);
        networkInfo.put("activePeers", activePeers);
        networkInfo.put("totalLinks", totalLinks);
        networkInfo.put("totalWords", totalWords);
        networkInfo.put("networkAvailable", totalPeers > 0);
        
        return objectMapper.valueToTree(networkInfo);
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
     * Uses Solr stats API which is publicly accessible
     */
    public JsonNode getIndexInfo() throws IOException {
        // Use Solr facet query to get index statistics (publicly accessible)
        String url = config.getServerUrl() + "/solr/select?q=*:*&rows=0&wt=json&facet=true&facet.field=host_s&facet.limit=10";
        JsonNode solrResult = executeGet(url);
        
        Map<String, Object> indexInfo = new java.util.LinkedHashMap<>();
        indexInfo.put("totalDocuments", solrResult.path("response").path("numFound").asLong());
        indexInfo.put("queryTime", solrResult.path("responseHeader").path("QTime").asInt());
        
        // Extract top hosts from facets if available
        JsonNode hostFacets = solrResult.path("facet_counts").path("facet_fields").path("host_s");
        if (!hostFacets.isMissingNode() && hostFacets.isArray()) {
            List<Map<String, Object>> topHosts = new java.util.ArrayList<>();
            for (int i = 0; i < hostFacets.size() - 1; i += 2) {
                Map<String, Object> hostEntry = new java.util.LinkedHashMap<>();
                hostEntry.put("host", hostFacets.get(i).asText());
                hostEntry.put("count", hostFacets.get(i + 1).asLong());
                topHosts.add(hostEntry);
            }
            indexInfo.put("topHosts", topHosts);
        }
        
        return objectMapper.valueToTree(indexInfo);
    }

    /**
     * Get peer information
     * Uses seedlist.json which returns JSON format
     */
    public JsonNode getPeers() throws IOException {
        String url = config.getServerUrl() + "/yacy/seedlist.json";
        return executeGet(url);
    }

    /**
     * Get performance statistics
     * Uses Solr ping and stats which are publicly accessible
     */
    public JsonNode getPerformance() throws IOException {
        // Use multiple Solr queries to measure performance
        long startTime = System.currentTimeMillis();
        String url = config.getServerUrl() + "/solr/select?q=*:*&rows=1&wt=json";
        JsonNode solrResult = executeGet(url);
        long responseTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> performance = new java.util.LinkedHashMap<>();
        performance.put("solrResponseTimeMs", responseTime);
        performance.put("solrQueryTimeMs", solrResult.path("responseHeader").path("QTime").asInt());
        performance.put("solrStatus", solrResult.path("responseHeader").path("status").asInt());
        performance.put("totalDocuments", solrResult.path("response").path("numFound").asLong());
        performance.put("serverAvailable", true);
        performance.put("timestamp", java.time.Instant.now().toString());
        
        return objectMapper.valueToTree(performance);
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
