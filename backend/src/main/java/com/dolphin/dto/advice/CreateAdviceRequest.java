package com.dolphin.dto.advice;

import com.dolphin.model.AdviceTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The student is derived from the target; it is never accepted from the client. */
public record CreateAdviceRequest(
        @NotNull(message = "Choose a project or LeetCode problem")
        AdviceTarget targetType,

        @NotBlank(message = "Choose a project or LeetCode problem")
        String targetId,

        @NotBlank(message = "Advice cannot be empty")
        @Size(max = 1000, message = "Advice must be at most 1000 characters")
        String message
) {
}
