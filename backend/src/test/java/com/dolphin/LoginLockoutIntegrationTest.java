package com.dolphin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

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
    private static final String LOCKED = "Too many login attempts. Please try again later.";

    @Autowired
    private MutableClock clock;

    private ResultActions attempt(String email, String password) throws Exception {
        return mvc.perform(withJson(post("/api/auth/login"), Map.of("email", email, "password", password)));
    }

    /** Three wrong passwords lock the account for 10 minutes; afterwards the correct password works again. */
    private void assertLockoutCycle(String email, String password) throws Exception {
        attempt(email, WRONG).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(GENERIC));
        attempt(email, WRONG).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(GENERIC));
        attempt(email, WRONG).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.message").value(LOCKED));
        // Locked: even the correct password is rejected, and further attempts do not extend the lock
        attempt(email, password).andExpect(status().isTooManyRequests());
        clock.advance(Duration.ofMinutes(5));
        attempt(email, WRONG).andExpect(status().isTooManyRequests());
        attempt(email, password).andExpect(status().isTooManyRequests());
        clock.advance(Duration.ofMinutes(5));
        // Exactly 10 minutes after locking it unlocks by itself
        attempt(email, password).andExpect(status().isOk());
        // The successful login reset the counter: two wrong passwords do not lock again
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, password).andExpect(status().isOk());
    }

    @Test
    void studentIsLockedOutAfterThreeWrongPasswords() throws Exception {
        assertLockoutCycle(registerStudent().email(), PASSWORD);
    }

    @Test
    void teacherIsLockedOutAfterThreeWrongPasswords() throws Exception {
        assertLockoutCycle(createTeacher().email(), PASSWORD);
    }

    @Test
    void adminIsLockedOutAfterThreeWrongPasswords() throws Exception {
        try {
            assertLockoutCycle(ADMIN_EMAIL, ADMIN_PASSWORD);
        } finally {
            clock.advance(Duration.ofMinutes(11)); // never leave the shared admin locked for other tests
        }
    }

    @Test
    void successfulLoginResetsTheCounter() throws Exception {
        String email = registerStudent().email();
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, PASSWORD).andExpect(status().isOk());
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void lockoutAfterExpiryStartsCountingFromScratch() throws Exception {
        String email = registerStudent().email();
        for (int i = 0; i < 3; i++) {
            attempt(email, WRONG);
        }
        clock.advance(Duration.ofMinutes(10));
        // The lock has expired, so this is attempt 1 of a new series, not attempt 4
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, WRONG).andExpect(status().isTooManyRequests());
        clock.advance(Duration.ofMinutes(10));
    }

    @Test
    void unknownEmailGetsTheSameGenericFailure() throws Exception {
        String email = uniqueEmail("nobody");
        for (int i = 0; i < 5; i++) {
            attempt(email, WRONG).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(GENERIC));
        }
    }

    @Test
    void invalidRequestsDoNotCountAsAttempts() throws Exception {
        String email = registerStudent().email();
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        attempt(email, WRONG).andExpect(status().isUnauthorized());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", email))).andExpect(status().isBadRequest());
        attempt(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void concurrentWrongPasswordsCannotBypassTheLimit() throws Exception {
        String email = registerStudent().email();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                tasks.add(() -> attempt(email, WRONG).andReturn().getResponse().getStatus());
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(tasks)) {
                statuses.add(f.get());
            }
            // Exactly two attempts may be answered as plain failures; every other one hits the lock
            assertThat(statuses.stream().filter(s -> s == 401).count()).isEqualTo(2);
            assertThat(statuses.stream().filter(s -> s == 429).count()).isEqualTo(10);
        } finally {
            pool.shutdown();
        }
        attempt(email, PASSWORD).andExpect(status().isTooManyRequests());
        clock.advance(Duration.ofMinutes(10));
        attempt(email, PASSWORD).andExpect(status().isOk());
    }
}
