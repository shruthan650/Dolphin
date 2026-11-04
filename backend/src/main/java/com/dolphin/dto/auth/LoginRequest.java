package com.dolphin.dto.auth;

import com.dolphin.validation.ValidEmailAddress;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @ValidEmailAddress
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
