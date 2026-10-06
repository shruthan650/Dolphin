package com.dolphin.model;

import java.time.Instant;
import java.time.LocalDate;

public class LeetCodeEntry {

    private String id;
    private String studentId;
    private String problemName;
    private String problemUrl;
    private Difficulty difficulty;
    private LeetCodeStatus status;
    private String topic;
    private LocalDate solvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public LeetCodeEntry copy() {
        LeetCodeEntry copy = new LeetCodeEntry();
        copy.id = id;
        copy.studentId = studentId;
        copy.problemName = problemName;
        copy.problemUrl = problemUrl;
        copy.difficulty = difficulty;
        copy.status = status;
        copy.topic = topic;
        copy.solvedAt = solvedAt;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        return copy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }

    public String getProblemUrl() { return problemUrl; }
    public void setProblemUrl(String problemUrl) { this.problemUrl = problemUrl; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public LeetCodeStatus getStatus() { return status; }
    public void setStatus(LeetCodeStatus status) { this.status = status; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public LocalDate getSolvedAt() { return solvedAt; }
    public void setSolvedAt(LocalDate solvedAt) { this.solvedAt = solvedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
