package com.dolphin.dto.advice;

import com.dolphin.model.AdviceTarget;

import java.time.Instant;

/** targetTitle is the project title or LeetCode problem name the advice is about. */
public record AdviceResponse(
        String id,
        AdviceTarget targetType,
        String targetId,
        String targetTitle,
        String studentId,
        String teacherId,
        String teacherName,
        String message,
        Instant createdAt
) {
}
