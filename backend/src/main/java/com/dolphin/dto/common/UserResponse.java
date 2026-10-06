package com.dolphin.dto.common;

import com.dolphin.model.Role;

import java.time.Instant;

/** Safe public view of a user. Never contains the password hash. */
public record UserResponse(
        String id,
        String name,
        String email,
        Role role,
        String githubUrl,
        String leetCodeUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
