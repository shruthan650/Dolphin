package com.dolphin.dto.account;

import jakarta.validation.constraints.NotBlank;

/** confirmation must be the literal word DELETE. */
public record DeleteAccountRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "Type DELETE to confirm")
        String confirmation
) {
}
