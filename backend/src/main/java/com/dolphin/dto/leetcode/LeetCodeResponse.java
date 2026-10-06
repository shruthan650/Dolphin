package com.dolphin.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.dolphin.model.Difficulty;
import com.dolphin.model.LeetCodeStatus;

import java.time.Instant;
import java.time.LocalDate;

/** studentName is only populated in teacher views. */
public record LeetCodeResponse(
        String id,
        String studentId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String studentName,
        String problemName,
        String problemUrl,
        Difficulty difficulty,
        LeetCodeStatus status,
        String topic,
        LocalDate solvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
