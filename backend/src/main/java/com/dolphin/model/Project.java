package com.dolphin.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Project {

    private String id;
    private String ownerId;
    private String title;
    private String description;
    private String githubUrl;
    private String liveUrl;
    private List<String> technologies = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public Project copy() {
        Project copy = new Project();
        copy.id = id;
        copy.ownerId = ownerId;
        copy.title = title;
        copy.description = description;
        copy.githubUrl = githubUrl;
        copy.liveUrl = liveUrl;
        copy.technologies = new ArrayList<>(technologies);
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        return copy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLiveUrl() { return liveUrl; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }

    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies == null ? new ArrayList<>() : new ArrayList<>(technologies);
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
