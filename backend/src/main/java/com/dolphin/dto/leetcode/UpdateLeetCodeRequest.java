package com.dolphin.dto.leetcode;

import com.dolphin.model.Difficulty;
import com.dolphin.model.LeetCodeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateLeetCodeRequest(
        @NotBlank(message = "Problem name is required")
        @Size(max = 150, message = "Problem name must be at most 150 characters")
        String problemName,

        @Size(max = 500, message = "Problem URL must be at most 500 characters")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Problem URL must start with http:// or https://")
        String problemUrl,

        @NotNull(message = "Difficulty is required")
        Difficulty difficulty,

        @NotNull(message = "Status is required")
        LeetCodeStatus status,

        @Size(max = 60, message = "Topic must be at most 60 characters")
        String topic,

        @PastOrPresent(message = "Solved date cannot be in the future")
        LocalDate solvedAt
) {
}
