package com.dolphin.dto.auth;

import com.dolphin.model.Role;

public record LoginResponse(
        String token,
        String userId,
        String name,
        String email,
        Role role,
        String githubUrl,
        String leetCodeUrl,
        long expiresIn
) {
}
