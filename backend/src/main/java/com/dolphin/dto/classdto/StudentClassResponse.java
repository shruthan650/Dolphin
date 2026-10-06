package com.dolphin.dto.classdto;

/** A student's view of a class they joined. Does not expose the list of other students. */
public record StudentClassResponse(
        String id,
        String className,
        int semester,
        String branch,
        String section,
        String classCode,
        String teacherName,
        int studentCount
) {
}
