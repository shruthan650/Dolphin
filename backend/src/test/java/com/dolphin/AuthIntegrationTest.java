package com.dolphin;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import com.dolphin.security.JwtService;
import com.dolphin.security.JwtTestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void validLoginReturnsTokenAndUserInfo() throws Exception {
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginIsCaseInsensitiveOnEmail() throws Exception {
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", "ADMIN@Dolphin.com", "password", ADMIN_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", ADMIN_EMAIL, "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void nonexistentEmailIsRejected() throws Exception {
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", "nobody@dolphin.com", "password", "whatever123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void blankLoginFieldsReturnValidationErrors() throws Exception {
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", "", "password", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void missingJwtReturns401() throws Exception {
        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedJwtReturns401() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

    @Test
    void expiredJwtReturns401() throws Exception {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        String expired = JwtTestTokens.expiredTokenFor(jwtService, admin);
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has expired"));
    }

    @Test
    void meReturnsCurrentUserWithoutPasswordHash() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(get("/api/auth/me"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(student.email()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrationAlwaysCreatesStudentEvenIfRoleIsSupplied() throws Exception {
        String email = uniqueEmail("sneaky");
        mvc.perform(withJson(post("/api/auth/register"), Map.of("name", "Sneaky", "email", email,
                        "password", PASSWORD, "confirmPassword", PASSWORD, "role", "ADMIN",
                        "githubUrl", GITHUB_URL, "leetCodeUrl", LEETCODE_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmail(email).orElseThrow().getRole())
                .isEqualTo(Role.STUDENT);
    }

    @Test
    void registrationRejectsMismatchedPasswords() throws Exception {
        mvc.perform(withJson(post("/api/auth/register"), Map.of("name", "X", "email", uniqueEmail("x"),
                        "password", PASSWORD, "confirmPassword", "Different123",
                        "githubUrl", GITHUB_URL, "leetCodeUrl", LEETCODE_URL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void passwordsAreStoredHashed() throws Exception {
        Account student = registerStudent();
        User stored = userRepository.findById(student.id()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stored.getPasswordHash())
                .isNotEqualTo(PASSWORD)
                .startsWith("$2");
    }

    @Test
    void deactivatedTeacherCannotLoginAndExistingTokenStopsWorking() throws Exception {
        Account teacher = createTeacher();
        mvc.perform(auth(withJson(patch("/api/admin/teachers/" + teacher.id() + "/status"),
                        Map.of("active", false)), admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", teacher.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("deactivated")));
        mvc.perform(auth(get("/api/teacher/dashboard"), teacher))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationStoresProfileLinksAndReturnsThemOnLoginAndMe() throws Exception {
        Account student = registerStudent();
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", student.email(), "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubUrl").value(GITHUB_URL))
                .andExpect(jsonPath("$.leetCodeUrl").value(LEETCODE_URL));
        mvc.perform(auth(get("/api/auth/me"), student))
                .andExpect(jsonPath("$.githubUrl").value(GITHUB_URL))
                .andExpect(jsonPath("$.leetCodeUrl").value(LEETCODE_URL));
    }

    @Test
    void registrationRequiresProfileLinks() throws Exception {
        mvc.perform(withJson(post("/api/auth/register"), Map.of("name", "X", "email", uniqueEmail("x"),
                        "password", PASSWORD, "confirmPassword", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.githubUrl", notNullValue()))
                .andExpect(jsonPath("$.errors.leetCodeUrl", notNullValue()));
    }

    @Test
    void registrationRejectsUrlsThatAreNotProfiles() throws Exception {
        mvc.perform(withJson(post("/api/auth/register"), Map.of("name", "X", "email", uniqueEmail("x"),
                        "password", PASSWORD, "confirmPassword", PASSWORD,
                        "githubUrl", "https://gitlab.com/octocat", "leetCodeUrl", "leetcode.com/u/octocat")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.githubUrl", containsString("GitHub")))
                .andExpect(jsonPath("$.errors.leetCodeUrl", containsString("LeetCode")));
    }

    @Test
    void studentCanUpdateProfileLinks() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(withJson(put("/api/student/profile-links"), Map.of(
                        "githubUrl", " https://github.com/new-name/ ", "leetCodeUrl", "https://leetcode.com/new_name")),
                        student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/new-name"))
                .andExpect(jsonPath("$.leetCodeUrl").value("https://leetcode.com/new_name"));
    }

    @Test
    void onlyStudentsCanUpdateProfileLinks() throws Exception {
        mvc.perform(auth(withJson(put("/api/student/profile-links"), Map.of(
                        "githubUrl", GITHUB_URL, "leetCodeUrl", LEETCODE_URL)), createTeacher()))
                .andExpect(status().isForbidden());
    }
}
