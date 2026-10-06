package com.dolphin.dto.teacher;

import java.util.List;

/**
 * A student as seen by a teacher, limited to what the teacher may see.
 *
 * @param classNames names of this teacher's classes the student is enrolled in
 */
public record TeacherStudentResponse(
        String id,
        String name,
        String email,
        boolean active,
        List<String> classNames,
        long projectCount,
        long leetCodeTotal,
        long leetCodeSolved
) {
}
