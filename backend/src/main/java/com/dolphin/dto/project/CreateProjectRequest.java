package com.dolphin.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * ownerId is deliberately absent: the owner always comes from the authenticated student. classId must be a class the
 * student is enrolled in.
 */
public record CreateProjectRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Size(max = 500, message = "GitHub URL must be at most 500 characters")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "GitHub URL must start with http:// or https://")
        String githubUrl,

        @Size(max = 500, message = "Live URL must be at most 500 characters")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Live URL must start with http:// or https://")
        String liveUrl,

        @Size(max = 20, message = "At most 20 technologies are allowed")
        List<@NotBlank(message = "Technology names cannot be blank")
             @Size(max = 40, message = "Technology names must be at most 40 characters") String> technologies,

        @NotBlank(message = "Class is required")
        String classId
) {
}
