package com.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClassIntegrationTest extends IntegrationTestSupport {

    private final Map<String, Object> classRequest = Map.of(
            "className", "5th Semester CS-IoT", "semester", 5, "branch", "CS-IoT", "section", "A");

    @Test
    void teacherCreatesClassWithGeneratedCodeAndOwnId() throws Exception {
        Account teacher = createTeacher();
        mvc.perform(auth(withJson(post("/api/classes"), Map.of(
                        "className", "5th Semester CS-IoT", "semester", 5, "branch", "CS-IoT", "section", "A",
                        "teacherId", "someone-else", "classCode", "DOLPHIN-HACKED")), teacher))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teacherId").value(teacher.id()))
                .andExpect(jsonPath("$.classCode", matchesPattern("DOLPHIN-[A-Z0-9]{5}")))
                .andExpect(jsonPath("$.studentCount").value(0));

        mvc.perform(auth(get("/api/classes/mine"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void studentCannotCreateClass() throws Exception {
        mvc.perform(auth(withJson(post("/api/classes"), classRequest), registerStudent()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidClassRequestIsRejected() throws Exception {
        mvc.perform(auth(withJson(post("/api/classes"), Map.of(
                        "className", "", "semester", 0, "branch", "", "section", "")), createTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.className").exists())
                .andExpect(jsonPath("$.errors.semester").exists());
    }

    @Test
    void studentJoinsClassAndTeacherSeesThem() throws Exception {
        Account teacher = createTeacher();
        JsonNode created = createClass(teacher);
        Account student = registerStudent();

        mvc.perform(auth(post("/api/classes/join/" + created.get("classCode").asText()), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classInfo.id").value(created.get("id").asText()))
                .andExpect(jsonPath("$.classInfo.studentCount").value(1));

        mvc.perform(auth(get("/api/student/classes"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(auth(get("/api/classes/" + created.get("id").asText()), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(1)))
                .andExpect(jsonPath("$.students[0].id").value(student.id()));
    }

    @Test
    void classCodeIsCaseInsensitive() throws Exception {
        JsonNode created = createClass(createTeacher());
        mvc.perform(auth(post("/api/classes/join/" + created.get("classCode").asText().toLowerCase()),
                        registerStudent()))
                .andExpect(status().isOk());
    }

    @Test
    void invalidClassCodeReturns404() throws Exception {
        mvc.perform(auth(post("/api/classes/join/DOLPHIN-NOPE0"), registerStudent()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void duplicateJoinReturns409() throws Exception {
        JsonNode created = createClass(createTeacher());
        Account student = registerStudent();
        String code = created.get("classCode").asText();
        joinClass(student, code);
        mvc.perform(auth(post("/api/classes/join/" + code), student))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You are already enrolled in this class."));
        mvc.perform(auth(get("/api/student/classes"), student))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void teacherCannotJoinClass() throws Exception {
        Account teacher = createTeacher();
        JsonNode created = createClass(teacher);
        mvc.perform(auth(post("/api/classes/join/" + created.get("classCode").asText()), teacher))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotViewOrModifyAnotherTeachersClass() throws Exception {
        Account teacherA = createTeacher();
        Account teacherB = createTeacher();
        String classId = createClass(teacherB).get("id").asText();

        mvc.perform(auth(get("/api/classes/" + classId), teacherA)).andExpect(status().isForbidden());
        mvc.perform(auth(withJson(put("/api/classes/" + classId), classRequest), teacherA))
                .andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/classes/" + classId), teacherA)).andExpect(status().isForbidden());

        mvc.perform(auth(withJson(put("/api/classes/" + classId), Map.of(
                        "className", "Renamed", "semester", 6, "branch", "CS", "section", "B")), teacherB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("Renamed"));
    }

    @Test
    void ownerDeletesClass() throws Exception {
        Account teacher = createTeacher();
        String classId = createClass(teacher).get("id").asText();
        mvc.perform(auth(delete("/api/classes/" + classId), teacher)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/classes/" + classId), teacher)).andExpect(status().isNotFound());
    }

    @Test
    void teacherRemovesStudentFromOwnClass() throws Exception {
        Account teacher = createTeacher();
        JsonNode created = createClass(teacher);
        Account student = registerStudent();
        joinClass(student, created.get("classCode").asText());
        String classId = created.get("id").asText();

        mvc.perform(auth(delete("/api/classes/" + classId + "/students/" + student.id()), teacher))
                .andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/classes/" + classId), teacher))
                .andExpect(jsonPath("$.students", hasSize(0)));
    }
}
