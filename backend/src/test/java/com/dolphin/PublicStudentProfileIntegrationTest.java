package com.dolphin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicStudentProfileIntegrationTest extends IntegrationTestSupport {

    @Test
    void publicProfileReturnsOnlyAllowlistedDataAndIndividuallySharedProjects() throws Exception {
        Account viewer = registerStudent();
        Account owner = registerStudent();
        setDisplayName(owner, "PublicOwner-" + UUID.randomUUID().toString().substring(0, 8));
        setProfileVisibility(owner, true);

        String privateProject = createProject(owner, "Private project");
        String publicProject = createProject(owner, "Public project");
        setProjectVisibility(owner, publicProject, true);
        createLeetCode(owner, "Publicly aggregated solve");

        JsonNode directory = read(mvc.perform(auth(get("/api/students/public")
                        .param("search", "PublicOwner-"), viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(1)))
                .andExpect(jsonPath("$.students[0].solvedProblems").value(1))
                .andExpect(jsonPath("$.students[0].publicProjectCount").value(1))
                .andReturn());
        String profileId = directory.get("students").get(0).get("profileId").asText();
        assertFalse(profileId.equals(owner.id()));

        var result = mvc.perform(auth(get("/api/students/" + profileId + "/public-profile"), viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(profileId))
                .andExpect(jsonPath("$.leetCodeStats.totalSolved").value(1))
                .andExpect(jsonPath("$.leetCodeStats.easySolved").value(1))
                .andExpect(jsonPath("$.projects", hasSize(1)))
                .andExpect(jsonPath("$.projects[0].title").value("Public project"))
                .andReturn();

        String response = result.getResponse().getContentAsString().toLowerCase();
        assertFalse(response.contains(owner.email().toLowerCase()));
        assertFalse(response.contains("contact@example.test"));
        assertFalse(response.contains("private-value"));
        assertFalse(response.contains("access_token"));
        assertFalse(response.contains("password"));
        assertFalse(response.contains("email"));
        assertFalse(response.contains("token"));
        assertFalse(response.contains("user_id"));
        assertFalse(response.contains("ownerid"));
        assertFalse(response.contains("classid"));
        assertFalse(response.contains(privateProject));
        assertFalse(response.contains(publicProject));
    }

    @Test
    void profilesArePrivateByDefaultAndCanBeMadePrivateAgain() throws Exception {
        Account viewer = registerStudent();
        Account owner = registerStudent();
        setDisplayName(owner, "PrivateOwner-" + UUID.randomUUID().toString().substring(0, 8));

        mvc.perform(auth(get("/api/student/public-profile-visibility"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false));
        mvc.perform(auth(get("/api/students/public").param("search", "PrivateOwner-"), viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(0)));
        mvc.perform(auth(get("/api/students/" + owner.id() + "/public-profile"), viewer))
                .andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/students/not-a-public-id/public-profile"), viewer))
                .andExpect(status().isNotFound());

        setProfileVisibility(owner, true);
        String profileId = read(mvc.perform(auth(get("/api/students/public")
                        .param("search", "PrivateOwner-"), viewer))
                .andExpect(status().isOk()).andReturn()).get("students").get(0).get("profileId").asText();
        mvc.perform(auth(get("/api/students/" + profileId + "/public-profile"), viewer))
                .andExpect(status().isOk());

        setProfileVisibility(owner, false);
        mvc.perform(auth(get("/api/students/" + profileId + "/public-profile"), viewer))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicDirectoryIsAuthenticatedStudentOnlySearchableAndPaginated() throws Exception {
        Account viewer = registerStudent();
        Account first = registerStudent();
        Account second = registerStudent();
        Account third = registerStudent();
        setDisplayName(first, "DirectoryCase-" + UUID.randomUUID().toString().substring(0, 8));
        setDisplayName(second, "DirectoryCase-" + UUID.randomUUID().toString().substring(0, 8));
        setDisplayName(third, "DirectoryCase-" + UUID.randomUUID().toString().substring(0, 8));
        setProfileVisibility(first, true);
        setProfileVisibility(second, true);
        setProfileVisibility(third, true);

        mvc.perform(get("/api/students/public"))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/students/public"), createTeacher()))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/students/public"), admin()))
                .andExpect(status().isForbidden());

        mvc.perform(auth(get("/api/students/public").param("search", "DirectoryCase-")
                        .param("page", "0").param("size", "2"), viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(auth(get("/api/students/public").param("search", "DirectoryCase-")
                        .param("page", "1").param("size", "2"), viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(1)));
        mvc.perform(auth(get("/api/students/public").param("search", "DirectoryCase-").param("size", "49"), viewer))
                .andExpect(status().isBadRequest());

        JsonNode found = read(mvc.perform(auth(get("/api/students/public")
                        .param("search", "DirectoryCase-").param("size", "1"), viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(1)))
                .andReturn());
        assertFalse(found.get("students").get(0).has("id"));
        assertFalse(found.get("students").get(0).has("email"));
    }

    @Test
    void visibilitySettingsOnlyOperateOnTheAuthenticatedStudentsOwnProfile() throws Exception {
        Account owner = registerStudent();
        Account other = registerStudent();
        setDisplayName(owner, "OwnerOnly-" + UUID.randomUUID().toString().substring(0, 8));
        setProfileVisibility(owner, true);

        mvc.perform(auth(withJson(put("/api/student/public-profile-visibility"),
                        Map.of("isPublic", false)), other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false));
        mvc.perform(auth(get("/api/students/public").param("search", "OwnerOnly-"), other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(1)))
                .andExpect(jsonPath("$.students[0].displayName", startsWith("OwnerOnly-")));
        mvc.perform(auth(put("/api/students/" + owner.id() + "/public-profile")
                        .contentType("application/json").content("{}"), other))
                .andExpect(status().isMethodNotAllowed());
    }

    private void setProfileVisibility(Account student, boolean isPublic) throws Exception {
        mvc.perform(auth(withJson(put("/api/student/public-profile-visibility"),
                        Map.of("isPublic", isPublic)), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(isPublic));
    }

    private void setDisplayName(Account student, String name) throws Exception {
        mvc.perform(auth(withJson(put("/api/users/me"), Map.of(
                        "name", name, "githubUrl", GITHUB_URL, "leetCodeUrl", LEETCODE_URL)), student))
                .andExpect(status().isOk());
    }

    private void setProjectVisibility(Account student, String projectId, boolean isPublic) throws Exception {
        mvc.perform(auth(withJson(put("/api/projects/" + projectId), Map.of(
                        "title", "Public project",
                        "description", "Project description; contact@example.test; api_key=private-value",
                        "githubUrl", "https://github.com/example/public?access_token=private-value",
                        "liveUrl", "",
                        "technologies", List.of("React"),
                        "publiclyVisible", isPublic)), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publiclyVisible").value(isPublic));
    }
}
