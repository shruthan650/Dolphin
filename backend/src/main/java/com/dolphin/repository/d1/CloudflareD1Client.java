package com.dolphin.repository.d1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Talks to D1 through Cloudflare's REST API:
 * {@code POST /accounts/{accountId}/d1/database/{databaseId}/query}. The API token needs the "D1 Edit" permission.
 */
public class CloudflareD1Client implements D1Client {

    private static final Logger log = LoggerFactory.getLogger(CloudflareD1Client.class);
    private static final String API_BASE = "https://api.cloudflare.com/client/v4";
    private static final TypeReference<List<Map<String, Object>>> ROWS = new TypeReference<>() { };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CloudflareD1Client(String accountId, String databaseId, String apiToken, Duration timeout,
                              ObjectMapper objectMapper) {
        this(RestClient.builder().requestFactory(requestFactory(timeout)), accountId, databaseId, apiToken,
                objectMapper);
    }

    /** Visible for tests, which bind a mock server to the builder. */
    CloudflareD1Client(RestClient.Builder builder, String accountId, String databaseId, String apiToken,
                       ObjectMapper objectMapper) {
        String account = clean(accountId, "CLOUDFLARE_ACCOUNT_ID");
        String database = clean(databaseId, "CLOUDFLARE_D1_DATABASE_ID");
        String token = clean(apiToken, "CLOUDFLARE_API_TOKEN");
        log.info("Cloudflare D1 client initialized");
        this.restClient = builder
                .baseUrl(API_BASE + "/accounts/" + account + "/d1/database/" + database + "/query")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Trims a setting and undoes common copy/paste mistakes in hosting dashboards: surrounding quotes and a pasted
     * "NAME=value" line.
     */
    static String clean(String value, String envVar) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envVar + " must be set (in backend/d1.env or as an environment variable)"
                    + " to use Cloudflare D1 storage");
        }
        String cleaned = value.trim();
        if (cleaned.startsWith(envVar + "=")) {
            cleaned = cleaned.substring(envVar.length() + 1).trim();
        }
        if (cleaned.length() >= 2 && (cleaned.startsWith("\"") && cleaned.endsWith("\"")
                || cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }

    @Override
    public List<Map<String, Object>> query(String sql, List<?> params) {
        JsonNode result = send(statement(sql, params));
        return objectMapper.convertValue(result.path(0).path("results"), ROWS);
    }

    @Override
    public void batch(List<D1Statement> statements) {
        if (statements.isEmpty()) {
            return;
        }
        send(Map.of("batch", statements.stream().map(s -> statement(s.sql(), s.params())).toList()));
    }

    /** @return the "result" array of the API response */
    private JsonNode send(Map<String, Object> body) {
        JsonNode response;
        try {
            response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            String hint = ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403
                    ? " - check CLOUDFLARE_API_TOKEN (needs D1 Edit) and CLOUDFLARE_ACCOUNT_ID" : "";
            throw new D1Exception("D1 request failed (HTTP " + ex.getStatusCode().value() + ")" + hint);
        } catch (RestClientException ex) {
            throw new D1Exception("Unable to reach Cloudflare D1");
        }
        if (response == null || !response.path("success").asBoolean(false)) {
            throw new D1Exception("D1 query failed");
        }
        return response.path("result");
    }

    private static Map<String, Object> statement(String sql, List<?> params) {
        List<String> wireParams = new ArrayList<>(params.size());
        params.forEach(p -> wireParams.add(D1Values.toParam(p)));
        Map<String, Object> statement = new HashMap<>();
        statement.put("sql", sql);
        statement.put("params", wireParams);
        return statement;
    }

}
