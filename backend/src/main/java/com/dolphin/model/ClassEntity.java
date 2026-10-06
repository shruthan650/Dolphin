package com.dolphin.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A class (course section) owned by a teacher. Named ClassEntity to avoid clashing with java.lang.Class.
 */
public class ClassEntity {

    private String id;
    private String className;
    private int semester;
    private String branch;
    private String section;
    private String classCode;
    private String teacherId;
    private Set<String> studentIds = new LinkedHashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    public ClassEntity copy() {
        ClassEntity copy = new ClassEntity();
        copy.id = id;
        copy.className = className;
        copy.semester = semester;
        copy.branch = branch;
        copy.section = section;
        copy.classCode = classCode;
        copy.teacherId = teacherId;
        copy.studentIds = new LinkedHashSet<>(studentIds);
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        return copy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public Set<String> getStudentIds() { return studentIds; }
    public void setStudentIds(Set<String> studentIds) {
        this.studentIds = studentIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(studentIds);
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
