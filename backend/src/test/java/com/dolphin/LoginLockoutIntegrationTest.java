package com.dolphin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginLockoutIntegrationTest extends IntegrationTestSupport {

    private static final String WRONG = "WrongPassword1";
    private static final String GENERIC = "Invalid email or password";
    private static final int ACCOUNT_IP_LIMIT = 8;

    @Autowired
    private MutableClock clock;

    private ResultActions attempt(String email, String password) throws Exception {
        MockHttpServletRequestBuilder request = withJson(post("/api/auth/login"),
                Map.of("email", email, "password", password));
        return mvc.perform(request.with(mockRequest -> {
            mockRequest.setRemoteAddr("test-" + email);
            return mockRequest;
        }));
    }

    @Test
    void throttlesRepeatedAttemptsWithoutLockingTheAccountAcrossAddresses() throws Exception {
        String email = registerStudent().email();
        for (int i = 0; i < ACCOUNT_IP_LIMIT; i++) {
            attempt(email, WRONG).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(GENERIC));
        }
        attempt(email, WRONG).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(GENERIC));

        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", email, "password", PASSWORD))
                        .with(request -> {
                            request.setRemoteAddr("test-alternate-network");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void successfulLoginResetsTheAccountAndAddressBucket() throws Exception {
        String email = registerStudent().email();
        for (int i = 0; i < 2; i++) {
            attempt(email, WRONG).andExpect(status().isUnauthorized());
        }
        attempt(email, PASSWORD).andExpect(status().isOk());
        for (int i = 0; i < ACCOUNT_IP_LIMIT; i++) {
            attempt(email, WRONG).andExpect(status().isUnauthorized());
        }
        attempt(email, WRONG).andExpect(status().isTooManyRequests());
    }

    @Test
    void unknownEmailReceivesGenericFailuresAndIsAlsoThrottled() throws Exception {
        String email = uniqueEmail("nobody");
        for (int i = 0; i < ACCOUNT_IP_LIMIT; i++) {
            attempt(email, WRONG).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(GENERIC));
        }
        attempt(email, WRONG).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(GENERIC));
    }

    @Test
    void invalidRequestsDoNotCountAsAttempts() throws Exception {
        String email = registerStudent().email();
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", email)))
                .andExpect(status().isBadRequest());
        attempt(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void concurrentRequestsCannotExceedTheAccountAddressLimit() throws Exception {
        String email = registerStudent().email();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                tasks.add(() -> attempt(email, WRONG).andReturn().getResponse().getStatus());
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : pool.invokeAll(tasks)) {
                statuses.add(future.get());
            }
            assertThat(statuses.stream().filter(status -> status == 401).count()).isEqualTo(ACCOUNT_IP_LIMIT);
            assertThat(statuses.stream().filter(status -> status == 429).count()).isEqualTo(4);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void rateLimitWindowExpires() throws Exception {
        String email = registerStudent().email();
        for (int i = 0; i < ACCOUNT_IP_LIMIT; i++) {
            attempt(email, WRONG).andExpect(status().isUnauthorized());
        }
        attempt(email, WRONG).andExpect(status().isTooManyRequests());
        clock.advance(Duration.ofMinutes(15));
        attempt(email, WRONG).andExpect(status().isUnauthorized());
    }
}
