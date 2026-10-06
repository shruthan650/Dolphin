package com.dolphin;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeetCodeIntegrationTest extends IntegrationTestSupport {

    private final Map<String, Object> update = Map.of(
            "problemName", "Two Sum II", "difficulty", "MEDIUM", "status", "ATTEMPTED", "topic", "Two pointers");

    @Test
    void studentCreatesEntryAndSolvedDateDefaultsToToday() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(withJson(post("/api/leetcode"), Map.of(
                        "problemName", "Two Sum", "problemUrl", "https://leetcode.com/problems/two-sum/",
                        "difficulty", "EASY", "status", "SOLVED", "topic", "Arrays",
                        "studentId", "someone-else")), student))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(student.id()))
                .andExpect(jsonPath("$.solvedAt").value(LocalDate.now().toString()));

        mvc.perform(auth(get("/api/leetcode/my"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void invalidEntryIsRejected() throws Exception {
        mvc.perform(auth(withJson(post("/api/leetcode"), Map.of(
                        "problemName", "", "difficulty", "IMPOSSIBLE", "status", "SOLVED")), registerStudent()))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(withJson(post("/api/leetcode"), Map.of("problemName", "X")), registerStudent()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.difficulty", notNullValue()))
                .andExpect(jsonPath("$.errors.status", notNullValue()));
    }

    @Test
    void studentUpdatesOwnEntry() throws Exception {
        Account student = registerStudent();
        String id = createLeetCode(student, "Two Sum");
        mvc.perform(auth(withJson(put("/api/leetcode/" + id), update), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemName").value("Two Sum II"))
                .andExpect(jsonPath("$.status").value("ATTEMPTED"))
                .andExpect(jsonPath("$.solvedAt").doesNotExist());
    }

    @Test
    void studentCannotUpdateOrDeleteAnotherStudentsEntry() throws Exception {
        Account owner = registerStudent();
        Account other = registerStudent();
        String id = createLeetCode(owner, "Two Sum");
        mvc.perform(auth(withJson(put("/api/leetcode/" + id), update), other))
                .andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/leetcode/" + id), other)).andExpect(status().isForbidden());
    }

    @Test
    void studentDeletesOwnEntry() throws Exception {
        Account student = registerStudent();
        String id = createLeetCode(student, "Two Sum");
        mvc.perform(auth(delete("/api/leetcode/" + id), student)).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/leetcode/" + id), student)).andExpect(status().isNotFound());
    }

    @Test
    void teacherCanViewProgressOfStudentInOwnClass() throws Exception {
        Account teacher = createTeacher();
        String code = createClass(teacher).get("classCode").asText();
        Account student = registerStudent();
        joinClass(student, code);
        createLeetCode(student, "Two Sum");
        createProject(student, "Smart Campus");

        mvc.perform(auth(get("/api/teacher/students/" + student.id() + "/leetcode"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].problemName").value("Two Sum"));
        mvc.perform(auth(get("/api/teacher/students/" + student.id() + "/projects"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(auth(get("/api/teacher/students/" + student.id()), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leetCodeStats.solved").value(1))
                .andExpect(jsonPath("$.projects", hasSize(1)))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(auth(get("/api/teacher/students"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].leetCodeSolved").value(1))
                .andExpect(jsonPath("$[0].projectCount").value(1));
        mvc.perform(auth(get("/api/teacher/dashboard"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClasses").value(1))
                .andExpect(jsonPath("$.totalStudents").value(1))
                .andExpect(jsonPath("$.totalProjects").value(1))
                .andExpect(jsonPath("$.problemsSolved").value(1))
                .andExpect(jsonPath("$.recentActivity", hasSize(2)));
    }

    @Test
    void teacherCannotViewStudentOutsideOwnClasses() throws Exception {
        Account teacherA = createTeacher();
        Account teacherB = createTeacher();
        Account student = registerStudent();
        joinClass(student, createClass(teacherB).get("classCode").asText());
        createLeetCode(student, "Two Sum");

        mvc.perform(auth(get("/api/teacher/students/" + student.id() + "/leetcode"), teacherA))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/teacher/students/" + student.id()), teacherA))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/teacher/students"), teacherA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(get("/api/teacher/leetcode"), teacherA))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void studentCannotUseTeacherEndpoints() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(get("/api/teacher/students/" + student.id() + "/leetcode"), student))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentDashboardReflectsOwnData() throws Exception {
        Account teacher = createTeacher();
        Account student = registerStudent();
        joinClass(student, createClass(teacher).get("classCode").asText());
        createProject(student, "P1");
        createLeetCode(student, "L1");

        mvc.perform(auth(get("/api/student/dashboard"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedClasses", hasSize(1)))
                .andExpect(jsonPath("$.projectCount").value(1))
                .andExpect(jsonPath("$.leetCodeStats.solved").value(1))
                .andExpect(jsonPath("$.recentProjects", hasSize(1)))
                .andExpect(jsonPath("$.recentLeetCode", hasSize(1)));
    }
}
