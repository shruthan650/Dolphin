package com.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeletionIntegrationTest extends IntegrationTestSupport {

    @Test
    void adminDeletesTeacherWithTheirClassesButStudentsKeepTheirAccounts() throws Exception {
        Account teacher = createTeacher();
        JsonNode cls = createClass(teacher);
        Account student = registerStudent();
        joinClass(student, cls.get("classCode").asText());
        createProject(student, "Kept project");

        mvc.perform(auth(delete("/api/admin/teachers/" + teacher.id()), admin()))
                .andExpect(status().isNoContent());

        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", teacher.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/teacher/dashboard"), teacher))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/student/classes"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        // The project of the deleted class is kept but no longer points at it
        mvc.perform(auth(get("/api/projects/my"), student))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].classId").doesNotExist());
    }

    @Test
    void adminCanOnlyDeleteTeachersThroughTheTeacherEndpoint() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(delete("/api/admin/teachers/" + student.id()), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(delete("/api/admin/teachers/does-not-exist"), admin()))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/admin/teachers/" + createTeacher().id()), createTeacher()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDeletesStudentWithAllTheirDataAndEnrolments() throws Exception {
        Account teacher = createTeacher();
        Account otherTeacher = createTeacher();
        JsonNode cls = createClass(teacher);
        JsonNode otherCls = createClass(otherTeacher);
        Account student = registerStudent();
        joinClass(student, cls.get("classCode").asText());
        joinClass(student, otherCls.get("classCode").asText());
        createProject(student, "Deleted project", cls.get("id").asText());
        createLeetCode(student, "Two Sum", otherCls.get("id").asText());

        mvc.perform(auth(delete("/api/admin/students/" + student.id()), admin()))
                .andExpect(status().isNoContent());

        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", student.email(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/projects/my"), student))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/teacher/students"), teacher))
                .andExpect(jsonPath("$", hasSize(0)));
        // Removed from every class, with no projects or LeetCode entries left behind
        mvc.perform(auth(get("/api/classes/" + otherCls.get("id").asText()), otherTeacher))
                .andExpect(jsonPath("$.students", hasSize(0)));
        mvc.perform(auth(get("/api/teacher/projects"), teacher))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(get("/api/teacher/leetcode"), otherTeacher))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void onlyAdminsCanDeleteStudentAccounts() throws Exception {
        Account teacher = createTeacher();
        JsonNode cls = createClass(teacher);
        Account student = registerStudent();
        joinClass(student, cls.get("classCode").asText());

        // The former teacher endpoint for deleting a whole account no longer exists
        mvc.perform(auth(delete("/api/teacher/students/" + student.id()), teacher))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(auth(delete("/api/admin/students/" + student.id()), teacher))
                .andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/admin/students/" + student.id()), registerStudent()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/admin/students/" + teacher.id()), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(delete("/api/admin/students/does-not-exist"), admin()))
                .andExpect(status().isNotFound());
        mvc.perform(withJson(post("/api/auth/login"), Map.of("email", student.email(), "password", PASSWORD)))
                .andExpect(status().isOk());
    }
}
