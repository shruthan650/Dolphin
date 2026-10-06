package com.dolphin.dto.admin;

import com.dolphin.model.Role;

import java.time.Instant;

/**
 * User row for admin listings.
 *
 * @param classCount classes taught (teacher) or joined (student); 0 for admins
 */
public record AdminUserResponse(
        String id,
        String name,
        String email,
        Role role,
        boolean active,
        int classCount,
        Instant createdAt
) {
}
