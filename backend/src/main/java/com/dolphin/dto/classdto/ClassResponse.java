package com.dolphin.dto.classdto;

import java.time.Instant;

public record ClassResponse(
        String id,
        String className,
        int semester,
        String branch,
        String section,
        String classCode,
        String teacherId,
        String teacherName,
        int studentCount,
        Instant createdAt,
        Instant updatedAt
) {
}
