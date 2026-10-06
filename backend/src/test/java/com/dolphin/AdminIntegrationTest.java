package com.dolphin;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationTest extends IntegrationTestSupport {

    private Map<String, Object> teacherRequest(String email) {
        return Map.of("name", "New Teacher", "email", email, "password", PASSWORD, "confirmPassword", PASSWORD);
    }

    @Test
    void adminCreatesTeacher() throws Exception {
        String email = uniqueEmail("t");
        mvc.perform(auth(withJson(post("/api/admin/teachers"), teacherRequest(email)), admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(auth(get("/api/admin/teachers"), admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(email)));
    }

    @Test
    void teacherCannotCreateTeacher() throws Exception {
        Account teacher = createTeacher();
        mvc.perform(auth(withJson(post("/api/admin/teachers"), teacherRequest(uniqueEmail("t"))), teacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void studentCannotCreateTeacher() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(withJson(post("/api/admin/teachers"), teacherRequest(uniqueEmail("t"))), student))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotCreateTeacher() throws Exception {
        mvc.perform(withJson(post("/api/admin/teachers"), teacherRequest(uniqueEmail("t"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateTeacherEmailIsRejected() throws Exception {
        String email = uniqueEmail("dup");
        Account admin = admin();
        mvc.perform(auth(withJson(post("/api/admin/teachers"), teacherRequest(email)), admin))
                .andExpect(status().isCreated());
        mvc.perform(auth(withJson(post("/api/admin/teachers"), teacherRequest(email.toUpperCase())), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidTeacherRequestReturnsFieldErrors() throws Exception {
        mvc.perform(auth(withJson(post("/api/admin/teachers"), Map.of(
                        "name", "", "email", "not-an-email", "password", "short", "confirmPassword", "short")), admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void adminDashboardIsComputedFromRepositories() throws Exception {
        createTeacher();
        registerStudent();
        mvc.perform(auth(get("/api/admin/dashboard"), admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalTeachers", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalStudents", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalClasses", notNullValue()))
                .andExpect(jsonPath("$.totalProjects", notNullValue()))
                .andExpect(jsonPath("$.totalLeetCodeEntries", notNullValue()));
    }

    @Test
    void studentAndTeacherCannotAccessAdminDashboard() throws Exception {
        mvc.perform(auth(get("/api/admin/dashboard"), registerStudent())).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/admin/dashboard"), createTeacher())).andExpect(status().isForbidden());
    }

    @Test
    void adminCannotUseTeacherOrStudentOnlyEndpoints() throws Exception {
        Account admin = admin();
        mvc.perform(auth(withJson(post("/api/classes"), Map.of(
                        "className", "X", "semester", 1, "branch", "CS", "section", "A")), admin))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/student/dashboard"), admin)).andExpect(status().isForbidden());
    }

    @Test
    void statusEndpointOnlyAppliesToTeachers() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(withJson(patch("/api/admin/teachers/" + student.id() + "/status"),
                        Map.of("active", false)), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(withJson(patch("/api/admin/teachers/does-not-exist/status"),
                        Map.of("active", false)), admin()))
                .andExpect(status().isNotFound());
    }
}
