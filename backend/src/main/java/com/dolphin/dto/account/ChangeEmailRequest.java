package com.dolphin.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The current password re-authenticates the user, because the email is the login identity. */
public record ChangeEmailRequest(
        @NotBlank(message = "New email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String newEmail,

        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
