package com.dolphin.dto.account;

import com.dolphin.validation.ValidEmailAddress;
import jakarta.validation.constraints.NotBlank;

/** The current password re-authenticates the user, because the email is the login identity. */
public record ChangeEmailRequest(
        @NotBlank(message = "New email is required")
        @ValidEmailAddress
        String newEmail,

        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
