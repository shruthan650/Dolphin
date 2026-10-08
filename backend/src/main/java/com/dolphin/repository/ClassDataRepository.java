package com.dolphin.repository;

/**
 * Operations that change class membership together with the class-scoped data that depends on it. Each method is
 * atomic: either everything is applied or nothing is.
 */
public interface ClassDataRepository {

    /**
     * Unenrols a student from one class and deletes their projects and LeetCode entries that belong to that class,
     * plus the advice on them. Their other classes, data of other classes, unassigned data and account are untouched.
     */
    void removeStudentFromClass(String classId, String studentId);

    /**
     * Deletes a class and its enrolments. Projects and LeetCode entries of that class are kept by their students and
     * become unassigned (classId = null), so no record points at a class that no longer exists.
     */
    void deleteClass(String classId);
}
