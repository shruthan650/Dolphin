package com.dolphin.model;

import java.time.Instant;

/** A teacher's advice to a student about one of the student's projects or LeetCode entries. */
public class Advice {

    private String id;
    private String teacherId;
    private String studentId;
    private AdviceTarget targetType;
    /** ID of the project or LeetCode entry. */
    private String targetId;
    private String message;
    private Instant createdAt;
    private Instant updatedAt;

    public Advice copy() {
        Advice copy = new Advice();
        copy.id = id;
        copy.teacherId = teacherId;
        copy.studentId = studentId;
        copy.targetType = targetType;
        copy.targetId = targetId;
        copy.message = message;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        return copy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public AdviceTarget getTargetType() { return targetType; }
    public void setTargetType(AdviceTarget targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
