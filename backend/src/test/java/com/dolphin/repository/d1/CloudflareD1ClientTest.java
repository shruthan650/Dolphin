package com.dolphin.repository.d1;

import com.dolphin.repository.d1.D1Client.D1Statement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Checks the request/response format of Cloudflare's D1 REST API without calling Cloudflare. */
class CloudflareD1ClientTest {

    private static final String URL =
            "https://api.cloudflare.com/client/v4/accounts/acc123/d1/database/db-456/query";

    private MockRestServiceServer server;
    private CloudflareD1Client client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CloudflareD1Client(builder, "acc123", "db-456", "secret-token", new ObjectMapper());
    }

    @Test
    void queryPostsSqlWithStringParamsAndReturnsRows() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andExpect(content().json("""
                        {"sql": "SELECT * FROM users WHERE active = ? AND created_at < ? AND github_url IS ?",
                         "params": ["1", "2026-10-06T10:00:00.000000000Z", null]}
                        """, true))
                .andRespond(withSuccess("""
                        {"success": true, "errors": [], "messages": [],
                         "result": [{"success": true, "meta": {}, "results": [{"id": "u1", "active": 1}]}]}
                        """, MediaType.APPLICATION_JSON));

        List<Map<String, Object>> rows = client.query(
                "SELECT * FROM users WHERE active = ? AND created_at < ? AND github_url IS ?",
                true, Instant.parse("2026-10-06T10:00:00Z"), null);

        assertThat(rows).containsExactly(Map.of("id", "u1", "active", 1));
        server.verify();
    }

    @Test
    void batchSendsAllStatementsInOneRequest() {
        server.expect(requestTo(URL))
                .andExpect(content().json("""
                        {"batch": [{"sql": "DELETE FROM class_students WHERE class_id = ?", "params": ["c1"]},
                                   {"sql": "DELETE FROM classes WHERE id = ?", "params": ["c1"]}]}
                        """, true))
                .andRespond(withSuccess("{\"success\": true, \"result\": [{}, {}], \"errors\": []}",
                        MediaType.APPLICATION_JSON));

        client.batch(List.of(
                D1Statement.of("DELETE FROM class_students WHERE class_id = ?", "c1"),
                D1Statement.of("DELETE FROM classes WHERE id = ?", "c1")));

        server.verify();
    }

    @Test
    void apiErrorsAreReportedWithCloudflaresMessage() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("""
                        {"success": false, "result": [], "errors": [{"code": 7500, "message": "no such table: users"}]}
                        """));

        assertThatThrownBy(() -> client.query("SELECT * FROM users"))
                .isInstanceOf(D1Exception.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("no such table: users");
    }

    @Test
    void missingCredentialsFailFast() {
        assertThatThrownBy(() -> new CloudflareD1Client(RestClient.builder(), "acc", "", "token", new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOUDFLARE_D1_DATABASE_ID");
    }
}
