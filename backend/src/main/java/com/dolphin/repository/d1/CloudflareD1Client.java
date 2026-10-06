package com.dolphin.repository.d1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        requireSetting(accountId, "CLOUDFLARE_ACCOUNT_ID");
        requireSetting(databaseId, "CLOUDFLARE_D1_DATABASE_ID");
        requireSetting(apiToken, "CLOUDFLARE_API_TOKEN");
        this.restClient = builder
                .baseUrl(API_BASE + "/accounts/" + accountId.trim() + "/d1/database/" + databaseId.trim() + "/query")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken.trim())
                .build();
        this.objectMapper = objectMapper;
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
            throw new D1Exception("D1 request failed (HTTP " + ex.getStatusCode().value() + "): "
                    + errorMessages(readQuietly(ex.getResponseBodyAsString())));
        } catch (RestClientException ex) {
            throw new D1Exception("Unable to reach Cloudflare D1: " + ex.getMessage(), ex);
        }
        if (response == null || !response.path("success").asBoolean(false)) {
            throw new D1Exception("D1 query failed: " + errorMessages(response));
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

    private JsonNode readQuietly(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String errorMessages(JsonNode response) {
        if (response == null || !response.path("errors").isArray() || response.path("errors").isEmpty()) {
            return "unknown error";
        }
        List<String> messages = new ArrayList<>();
        response.path("errors").forEach(e -> messages.add(e.path("message").asText(e.toString())));
        return String.join("; ", messages);
    }

    private static void requireSetting(String value, String envVar) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envVar + " must be set (in backend/d1.env or as an environment variable) to use Cloudflare D1 storage");
        }
    }
}
