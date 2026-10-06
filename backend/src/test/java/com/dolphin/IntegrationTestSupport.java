package com.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for MockMvc integration tests. Each test creates its own users with unique
 * emails, so tests stay independent even though the in-memory store is shared by the context.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    protected static final String ADMIN_EMAIL = "admin@dolphin.com";
    protected static final String ADMIN_PASSWORD = "Admin@12345";
    protected static final String PASSWORD = "Password123";
    protected static final String GITHUB_URL = "https://github.com/octocat";
    protected static final String LEETCODE_URL = "https://leetcode.com/u/octocat";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected record Account(String id, String email, String token) {
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder builder, Object body)
            throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, Account account) {
        return builder.header("Authorization", "Bearer " + account.token());
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(withJson(post("/api/auth/login"), Map.of("email", email, "password", password)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).get("token").asText();
    }

    protected Account admin() throws Exception {
        return new Account(null, ADMIN_EMAIL, login(ADMIN_EMAIL, ADMIN_PASSWORD));
    }

    protected Account createTeacher() throws Exception {
        String email = uniqueEmail("teacher");
        MvcResult result = mvc.perform(auth(withJson(post("/api/admin/teachers"), Map.of(
                        "name", "Teacher " + email, "email", email,
                        "password", PASSWORD, "confirmPassword", PASSWORD)), admin()))
                .andExpect(status().isCreated())
                .andReturn();
        return new Account(read(result).get("id").asText(), email, login(email, PASSWORD));
    }

    protected Account registerStudent() throws Exception {
        String email = uniqueEmail("student");
        MvcResult result = mvc.perform(withJson(post("/api/auth/register"), Map.of(
                        "name", "Student " + email, "email", email,
                        "password", PASSWORD, "confirmPassword", PASSWORD,
                        "githubUrl", GITHUB_URL, "leetCodeUrl", LEETCODE_URL)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = read(result);
        return new Account(body.get("userId").asText(), email, body.get("token").asText());
    }

    /** @return the created class JSON */
    protected JsonNode createClass(Account teacher) throws Exception {
        MvcResult result = mvc.perform(auth(withJson(post("/api/classes"), Map.of(
                        "className", "5th Semester CS-IoT", "semester", 5, "branch", "CS-IoT", "section", "A")),
                        teacher))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result);
    }

    protected void joinClass(Account student, String classCode) throws Exception {
        mvc.perform(auth(post("/api/classes/join/" + classCode), student))
                .andExpect(status().isOk());
    }

    protected String createProject(Account student, String title) throws Exception {
        MvcResult result = mvc.perform(auth(withJson(post("/api/projects"), Map.of(
                        "title", title, "description", "desc",
                        "githubUrl", "https://github.com/example/repo",
                        "technologies", java.util.List.of("React", "Spring Boot"))), student))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).get("id").asText();
    }

    protected String createLeetCode(Account student, String problem) throws Exception {
        MvcResult result = mvc.perform(auth(withJson(post("/api/leetcode"), Map.of(
                        "problemName", problem, "difficulty", "EASY", "status", "SOLVED", "topic", "Arrays")),
                        student))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).get("id").asText();
    }
}
