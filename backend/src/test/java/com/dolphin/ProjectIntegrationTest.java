package com.dolphin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectIntegrationTest extends IntegrationTestSupport {

    private final Map<String, Object> update = Map.of(
            "title", "Updated title", "description", "new", "githubUrl", "https://github.com/x/y",
            "liveUrl", "", "technologies", List.of("Java"));

    @Test
    void studentCreatesProjectOwnedByThemselves() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(withJson(post("/api/projects"), Map.of(
                        "title", "Smart Campus", "description", "Smart campus management system",
                        "githubUrl", "https://github.com/x/smart-campus", "liveUrl", "https://smart.example.com",
                        "technologies", List.of("React", "Spring Boot", "Java"),
                        "ownerId", "someone-else", "classId", classIdFor(student))), student))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(student.id()))
                .andExpect(jsonPath("$.title").value("Smart Campus"))
                .andExpect(jsonPath("$.technologies", hasSize(3)));

        mvc.perform(auth(get("/api/projects/my"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void teacherCannotCreateProject() throws Exception {
        mvc.perform(auth(withJson(post("/api/projects"), Map.of("title", "X", "classId", "any")), createTeacher()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidProjectIsRejected() throws Exception {
        mvc.perform(auth(withJson(post("/api/projects"), Map.of(
                        "title", " ", "githubUrl", "ftp://nope", "liveUrl", "javascript:alert(1)")), registerStudent()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.githubUrl").exists())
                .andExpect(jsonPath("$.errors.liveUrl").exists())
                .andExpect(jsonPath("$.errors.classId").exists());
    }

    @Test
    void studentUpdatesOwnProject() throws Exception {
        Account student = registerStudent();
        String id = createProject(student, "Original");
        mvc.perform(auth(withJson(put("/api/projects/" + id), update), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.liveUrl").doesNotExist());
    }

    @Test
    void studentCannotUpdateOrDeleteAnotherStudentsProject() throws Exception {
        Account owner = registerStudent();
        Account other = registerStudent();
        String id = createProject(owner, "Owner project");

        mvc.perform(auth(withJson(put("/api/projects/" + id), update), other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to modify this project"));
        mvc.perform(auth(delete("/api/projects/" + id), other)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/projects/" + id), other)).andExpect(status().isForbidden());

        mvc.perform(auth(get("/api/projects/" + id), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Owner project"));
    }

    @Test
    void studentDeletesOwnProject() throws Exception {
        Account student = registerStudent();
        String id = createProject(student, "To delete");
        mvc.perform(auth(delete("/api/projects/" + id), student)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/projects/" + id), student)).andExpect(status().isNotFound());
    }

    @Test
    void nonexistentProjectReturns404() throws Exception {
        Account student = registerStudent();
        mvc.perform(auth(withJson(put("/api/projects/does-not-exist"), update), student))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(auth(delete("/api/projects/does-not-exist"), student)).andExpect(status().isNotFound());
    }
}
