package com.dolphin.dto.classdto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** teacherId is deliberately absent: the owner always comes from the authenticated teacher. */
public record CreateClassRequest(
        @NotBlank(message = "Class name is required")
        @Size(max = 100, message = "Class name must be at most 100 characters")
        String className,

        @NotNull(message = "Semester is required")
        @Min(value = 1, message = "Semester must be between 1 and 12")
        @Max(value = 12, message = "Semester must be between 1 and 12")
        Integer semester,

        @NotBlank(message = "Branch is required")
        @Size(max = 50, message = "Branch must be at most 50 characters")
        String branch,

        @NotBlank(message = "Section is required")
        @Size(max = 10, message = "Section must be at most 10 characters")
        String section
) {
}
