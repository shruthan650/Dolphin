package com.dolphin.service;

import com.dolphin.model.ClassEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which class-scoped student records a teacher may see, given a set of the teacher's classes: records of those classes
 * whose owner is enrolled in them, plus a student's unassigned records (classId null: legacy data or a deleted class)
 * when the student is enrolled in at least one of the classes.
 */
final class TeacherScope {

    private final Map<String, ClassEntity> classesById = new HashMap<>();
    private final Set<String> studentIds = new HashSet<>();

    private TeacherScope(List<ClassEntity> classes) {
        for (ClassEntity c : classes) {
            classesById.put(c.getId(), c);
            studentIds.addAll(c.getStudentIds());
        }
    }

    static TeacherScope of(List<ClassEntity> classes) {
        return new TeacherScope(classes);
    }

    boolean canSee(String ownerId, String classId) {
        if (classId == null) {
            return studentIds.contains(ownerId);
        }
        ClassEntity c = classesById.get(classId);
        return c != null && c.getStudentIds().contains(ownerId);
    }

    /** @return the class name, or null for unassigned records and classes outside the scope */
    String className(String classId) {
        ClassEntity c = classId == null ? null : classesById.get(classId);
        return c == null ? null : c.getClassName();
    }
}
