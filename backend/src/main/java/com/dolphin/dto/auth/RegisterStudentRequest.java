package com.dolphin.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public student self-registration. The role is always STUDENT and cannot be chosen by the client. */
public record RegisterStudentRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "Please confirm the password")
        String confirmPassword,

        @NotBlank(message = "GitHub profile URL is required")
        @Size(max = 200, message = "GitHub profile URL must be at most 200 characters")
        @Pattern(regexp = ProfileLinkPatterns.GITHUB, message = ProfileLinkPatterns.GITHUB_MESSAGE)
        String githubUrl,

        @NotBlank(message = "LeetCode profile URL is required")
        @Size(max = 200, message = "LeetCode profile URL must be at most 200 characters")
        @Pattern(regexp = ProfileLinkPatterns.LEETCODE, message = ProfileLinkPatterns.LEETCODE_MESSAGE)
        String leetCodeUrl
) {
}
