package com.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdviceIntegrationTest extends IntegrationTestSupport {

    private record Setup(Account teacher, Account student, String projectId, String leetCodeId) {
    }

    private Setup enrolledStudentWithWork() throws Exception {
        Account teacher = createTeacher();
        Account student = registerStudent();
        joinClass(student, createClass(teacher).get("classCode").asText());
        return new Setup(teacher, student, createProject(student, "Weather App"), createLeetCode(student, "Two Sum"));
    }

    private JsonNode giveAdvice(Account teacher, String type, String targetId, String message) throws Exception {
        return read(mvc.perform(auth(withJson(post("/api/advice"),
                        Map.of("targetType", type, "targetId", targetId, "message", message)), teacher))
                .andExpect(status().isCreated())
                .andReturn());
    }

    @Test
    void teacherAdvisesOnProjectAndLeetCodeAndStudentSeesIt() throws Exception {
        Setup s = enrolledStudentWithWork();

        mvc.perform(auth(withJson(post("/api/advice"), Map.of("targetType", "PROJECT", "targetId", s.projectId(),
                        "message", "  Add unit tests and a README.  ")), s.teacher()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.targetTitle").value("Weather App"))
                .andExpect(jsonPath("$.studentId").value(s.student().id()))
                .andExpect(jsonPath("$.teacherName", notNullValue()))
                .andExpect(jsonPath("$.message").value("Add unit tests and a README."));
        giveAdvice(s.teacher(), "LEETCODE", s.leetCodeId(), "Try the hash map approach for O(n).");

        mvc.perform(auth(get("/api/advice/my"), s.student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].targetType").value("LEETCODE"))
                .andExpect(jsonPath("$[0].targetTitle").value("Two Sum"))
                .andExpect(jsonPath("$[1].targetType").value("PROJECT"));
        mvc.perform(auth(get("/api/teacher/students/" + s.student().id()), s.teacher()))
                .andExpect(jsonPath("$.advice", hasSize(2)));
    }

    @Test
    void onlyTeachersOfTheStudentCanAdvise() throws Exception {
        Setup s = enrolledStudentWithWork();
        Map<String, String> body = Map.of("targetType", "PROJECT", "targetId", s.projectId(), "message", "Nice");

        mvc.perform(auth(withJson(post("/api/advice"), body), createTeacher()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(withJson(post("/api/advice"), body), s.student()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/advice/my"), s.teacher()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adviceIsValidated() throws Exception {
        Setup s = enrolledStudentWithWork();
        mvc.perform(auth(withJson(post("/api/advice"),
                        Map.of("targetType", "PROJECT", "targetId", s.projectId(), "message", " ")), s.teacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message", notNullValue()));
        mvc.perform(auth(withJson(post("/api/advice"),
                        Map.of("targetType", "LEETCODE", "targetId", "missing", "message", "Hi")), s.teacher()))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyTheAuthorCanDeleteAdvice() throws Exception {
        Setup s = enrolledStudentWithWork();
        String adviceId = giveAdvice(s.teacher(), "PROJECT", s.projectId(), "Deploy it").get("id").asText();

        mvc.perform(auth(delete("/api/advice/" + adviceId), createTeacher()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/advice/" + adviceId), s.teacher()))
                .andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/advice/my"), s.student()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deletingAProjectRemovesItsAdvice() throws Exception {
        Setup s = enrolledStudentWithWork();
        giveAdvice(s.teacher(), "PROJECT", s.projectId(), "Add screenshots");
        giveAdvice(s.teacher(), "LEETCODE", s.leetCodeId(), "Good job");

        mvc.perform(auth(delete("/api/projects/" + s.projectId()), s.student()))
                .andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/advice/my"), s.student()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType").value("LEETCODE"));
    }
}
