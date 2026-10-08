package com.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Projects and LeetCode entries belong to one class; leaving or being removed deletes only that class's data. */
class ClassScopedDataIntegrationTest extends IntegrationTestSupport {

    /** A student in class A (teacher A) and class B (teacher B) with data in both, plus advice on the A project. */
    private record Setup(Account teacherA, Account teacherB, String classA, String classB, Account student,
                         String projectA, String projectB, String leetA, String leetB) {
    }

    private Setup setup() throws Exception {
        Account teacherA = createTeacher();
        Account teacherB = createTeacher();
        JsonNode classA = createClass(teacherA);
        JsonNode classB = createClass(teacherB);
        Account student = registerStudent();
        joinClass(student, classA.get("classCode").asText());
        joinClass(student, classB.get("classCode").asText());
        String a = classA.get("id").asText();
        String b = classB.get("id").asText();
        Setup s = new Setup(teacherA, teacherB, a, b, student,
                createProject(student, "Project A", a), createProject(student, "Project B", b),
                createLeetCode(student, "Problem A", a), createLeetCode(student, "Problem B", b));
        mvc.perform(auth(withJson(post("/api/advice"), Map.of("targetType", "PROJECT", "targetId", s.projectA(),
                        "message", "Add tests")), teacherA))
                .andExpect(status().isCreated());
        return s;
    }

    private void assertOnlyClassBRemains(Setup s) throws Exception {
        mvc.perform(auth(get("/api/student/classes"), s.student()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(s.classB()));
        mvc.perform(auth(get("/api/projects/my"), s.student()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(s.projectB()));
        mvc.perform(auth(get("/api/leetcode/my"), s.student()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(s.leetB()));
        mvc.perform(auth(get("/api/advice/my"), s.student()))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(get("/api/classes/" + s.classA()), s.teacherA()))
                .andExpect(jsonPath("$.students", hasSize(0)));
        mvc.perform(auth(get("/api/classes/" + s.classB()), s.teacherB()))
                .andExpect(jsonPath("$.students", hasSize(1)))
                .andExpect(jsonPath("$.students[0].projectCount").value(1));
        // Global profile is untouched
        mvc.perform(auth(get("/api/users/me"), s.student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubUrl").value(GITHUB_URL));
        login(s.student().email(), PASSWORD);
    }

    @Test
    void eachTeacherSeesOnlyTheirClassData() throws Exception {
        Setup s = setup();
        mvc.perform(auth(get("/api/teacher/projects"), s.teacherA()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(s.projectA()))
                .andExpect(jsonPath("$[0].className").exists());
        mvc.perform(auth(get("/api/teacher/leetcode"), s.teacherB()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(s.leetB()));
        mvc.perform(auth(get("/api/teacher/students/" + s.student().id()), s.teacherB()))
                .andExpect(jsonPath("$.projects", hasSize(1)))
                .andExpect(jsonPath("$.advice", hasSize(0)));
        // Teacher B cannot advise on class A work
        mvc.perform(auth(withJson(post("/api/advice"), Map.of("targetType", "LEETCODE", "targetId", s.leetA(),
                        "message", "Nope")), s.teacherB()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/projects/my"), s.student()))
                .andExpect(jsonPath("$[*].classId", containsInAnyOrder(s.classA(), s.classB())));
    }

    @Test
    void teacherRemovingStudentDeletesOnlyThatClassData() throws Exception {
        Setup s = setup();
        mvc.perform(auth(delete("/api/classes/" + s.classA() + "/students/" + s.student().id()), s.teacherA()))
                .andExpect(status().isNoContent());
        assertOnlyClassBRemains(s);
        mvc.perform(auth(delete("/api/classes/" + s.classA() + "/students/" + s.student().id()), s.teacherA()))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentLeavingDeletesOnlyThatClassData() throws Exception {
        Setup s = setup();
        mvc.perform(auth(delete("/api/student/classes/" + s.classA()), s.student()))
                .andExpect(status().isNoContent());
        assertOnlyClassBRemains(s);
    }

    @Test
    void leavingRequiresMembershipOfAnExistingClass() throws Exception {
        Setup s = setup();
        JsonNode otherClass = createClass(s.teacherA());
        mvc.perform(auth(delete("/api/student/classes/" + otherClass.get("id").asText()), s.student()))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/student/classes/does-not-exist"), s.student()))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/student/classes/" + s.classA()), s.teacherA()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/projects/my"), s.student())).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void teacherCannotRemoveStudentFromAnotherTeachersClass() throws Exception {
        Setup s = setup();
        mvc.perform(auth(delete("/api/classes/" + s.classB() + "/students/" + s.student().id()), s.teacherA()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/classes/" + s.classB() + "/students/" + s.student().id()), registerStudent()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/projects/my"), s.student())).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void studentCanOnlyAddWorkToJoinedClasses() throws Exception {
        Setup s = setup();
        JsonNode notJoined = createClass(createTeacher());
        String classId = notJoined.get("id").asText();
        mvc.perform(auth(withJson(post("/api/projects"), Map.of("title", "Sneaky", "classId", classId)), s.student()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(withJson(post("/api/leetcode"), Map.of("problemName", "Sneaky", "difficulty", "EASY",
                        "status", "SOLVED", "classId", classId)), s.student()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(withJson(put("/api/projects/" + s.projectA()), Map.of("title", "Moved",
                        "classId", classId)), s.student()))
                .andExpect(status().isForbidden());
        // Moving work between the student's own classes is allowed
        mvc.perform(auth(withJson(put("/api/projects/" + s.projectA()), Map.of("title", "Moved",
                        "classId", s.classB())), s.student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(s.classB()));
    }

    @Test
    void deletingAClassKeepsItsDataAsUnassigned() throws Exception {
        Setup s = setup();
        mvc.perform(auth(delete("/api/classes/" + s.classA()), s.teacherA()))
                .andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/student/classes"), s.student())).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(auth(get("/api/projects/my"), s.student())).andExpect(jsonPath("$", hasSize(2)));
        // Unassigned work stays visible to the student's remaining teacher, and is never deleted by leaving B
        mvc.perform(auth(get("/api/teacher/projects"), s.teacherB())).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(auth(delete("/api/student/classes/" + s.classB()), s.student()))
                .andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/projects/my"), s.student()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(s.projectA()));
    }

    @Test
    void adminDeletingTeacherUnenrolsStudentsAndKeepsTheirData() throws Exception {
        Setup s = setup();
        mvc.perform(auth(delete("/api/admin/teachers/" + s.teacherA().id()), admin()))
                .andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/student/classes"), s.student()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(s.classB()));
        mvc.perform(auth(get("/api/projects/my"), s.student())).andExpect(jsonPath("$", hasSize(2)));
        // Advice written by the deleted teacher is gone
        mvc.perform(auth(get("/api/advice/my"), s.student())).andExpect(jsonPath("$", hasSize(0)));
    }
}
