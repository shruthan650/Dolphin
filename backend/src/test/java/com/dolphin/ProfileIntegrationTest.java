package com.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileIntegrationTest extends IntegrationTestSupport {

    private static final String NEW_PASSWORD = "NewPassword456";

    private Account withToken(Account account, JsonNode loginResponse) {
        return new Account(account.id(), loginResponse.get("email").asText(), loginResponse.get("token").asText());
    }

    @Test
    void everyRoleReadsOwnProfileWithoutSecrets() throws Exception {
        for (Account account : new Account[] {registerStudent(), createTeacher(), admin()}) {
            mvc.perform(auth(get("/api/users/me"), account))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(account.email()))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.tokenVersion").doesNotExist());
        }
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentChangesNameAndLinks() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(withJson(put("/api/users/me"), Map.of("name", "  Asha Rao ",
                        "githubUrl", "https://github.com/asha/", "leetCodeUrl", "https://leetcode.com/u/asha")), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Asha Rao"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/asha"));
        // Students must keep their coding profiles
        mvc.perform(auth(withJson(put("/api/users/me"), Map.of("name", "Asha")), student))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(withJson(put("/api/users/me"), Map.of("name", " ")), student))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teacherChangesNameWithoutLinks() throws Exception {
        Account teacher = createTeacher();
        mvc.perform(auth(withJson(put("/api/users/me"), Map.of("name", "Dr. Teacher")), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Teacher"));
    }

    @Test
    void profileUpdatesOnlyEverTouchTheCallersOwnAccount() throws Exception {
        Account student = registerStudent();
        Account other = registerStudent();
        // There is no endpoint addressing another user; a smuggled id is ignored
        mvc.perform(auth(withJson(put("/api/users/me"), Map.of("id", other.id(), "name", "Hacked",
                        "githubUrl", GITHUB_URL, "leetCodeUrl", LEETCODE_URL)), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.id()));
        mvc.perform(auth(get("/api/users/me"), other))
                .andExpect(jsonPath("$.name").value("Student " + other.email()));
        mvc.perform(auth(withJson(put("/api/users/" + other.id()), Map.of("name", "Hacked")), student))
                .andExpect(status().isNotFound());
    }

    @Test
    void changingEmailRequiresPasswordAndReplacesTheLoginIdentity() throws Exception {
        Account student = registerStudent();
        String newEmail = uniqueEmail("renamed");
        mvc.perform(auth(withJson(put("/api/users/me/email"), Map.of("newEmail", newEmail,
                        "currentPassword", "wrong-password")), student))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));

        JsonNode response = read(mvc.perform(auth(withJson(put("/api/users/me/email"), Map.of(
                        "newEmail", newEmail.toUpperCase(), "currentPassword", PASSWORD)), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andReturn());

        // The old token is revoked, the new one works, and only the new email can log in
        mvc.perform(auth(get("/api/users/me"), student)).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/users/me"), withToken(student, response))).andExpect(status().isOk());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", student.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        login(newEmail, PASSWORD);
    }

    @Test
    void duplicateOrInvalidEmailIsRejected() throws Exception {
        Account student = registerStudent();
        Account other = registerStudent();
        mvc.perform(auth(withJson(put("/api/users/me/email"), Map.of("newEmail", other.email(),
                        "currentPassword", PASSWORD)), student))
                .andExpect(status().isConflict());
        mvc.perform(auth(withJson(put("/api/users/me/email"), Map.of("newEmail", "not-an-email",
                        "currentPassword", PASSWORD)), student))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newEmail").exists());
        login(student.email(), PASSWORD);
    }

    @Test
    void changingPasswordVerifiesTheCurrentOneAndSignsOutOtherSessions() throws Exception {
        Account student = registerStudent();
        String otherSession = login(student.email(), PASSWORD);
        mvc.perform(auth(withJson(patch("/api/users/me/password"), Map.of("currentPassword", "wrong-password",
                        "newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD)), student))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(withJson(patch("/api/users/me/password"), Map.of("currentPassword", PASSWORD,
                        "newPassword", NEW_PASSWORD, "confirmPassword", "Mismatch123")), student))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
        mvc.perform(auth(withJson(patch("/api/users/me/password"), Map.of("currentPassword", PASSWORD,
                        "newPassword", "short", "confirmPassword", "short")), student))
                .andExpect(status().isBadRequest());

        JsonNode response = read(mvc.perform(auth(withJson(patch("/api/users/me/password"), Map.of(
                        "currentPassword", PASSWORD, "newPassword", NEW_PASSWORD,
                        "confirmPassword", NEW_PASSWORD)), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn());

        mvc.perform(auth(get("/api/users/me"), student)).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/users/me"), new Account(student.id(), student.email(), otherSession)))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/users/me"), withToken(student, response))).andExpect(status().isOk());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", student.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        login(student.email(), NEW_PASSWORD);
    }

    @Test
    void studentDeletesOwnAccountAfterTypingDelete() throws Exception {
        Account teacher = createTeacher();
        JsonNode cls = createClass(teacher);
        Account student = registerStudent();
        joinClass(student, cls.get("classCode").asText());
        createProject(student, "Gone");

        mvc.perform(auth(withJson(delete("/api/users/me"), Map.of("currentPassword", PASSWORD,
                        "confirmation", "delete")), student))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(withJson(delete("/api/users/me"), Map.of("currentPassword", "wrong-password",
                        "confirmation", "DELETE")), student))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(withJson(delete("/api/users/me"), Map.of("currentPassword", PASSWORD,
                        "confirmation", "DELETE")), student))
                .andExpect(status().isNoContent());

        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", student.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/classes/" + cls.get("id").asText()), teacher))
                .andExpect(jsonPath("$.students", hasSize(0)));
        mvc.perform(auth(get("/api/teacher/projects"), teacher))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void teacherDeletesOwnAccountButAdminCannot() throws Exception {
        Account teacher = createTeacher();
        mvc.perform(auth(withJson(delete("/api/users/me"), Map.of("currentPassword", PASSWORD,
                        "confirmation", "DELETE")), teacher))
                .andExpect(status().isNoContent());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", teacher.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());

        mvc.perform(auth(withJson(delete("/api/users/me"), Map.of("currentPassword", ADMIN_PASSWORD,
                        "confirmation", "DELETE")), admin()))
                .andExpect(status().isForbidden());
        login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    void registrationAlwaysCreatesAStudent() throws Exception {
        String email = uniqueEmail("sneaky");
        mvc.perform(withJson(post("/api/auth/register"), Map.of("name", "Sneaky", "email", email,
                        "password", PASSWORD, "confirmPassword", PASSWORD, "role", "ADMIN",
                        "githubUrl", GITHUB_URL, "leetCodeUrl", LEETCODE_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
