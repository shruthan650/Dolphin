package com.dolphin.dto.account;

import com.dolphin.dto.auth.ProfileLinkPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The signed-in user's own profile. The profile links are required for students and optional for other roles. */
public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 200, message = "GitHub profile URL must be at most 200 characters")
        @Pattern(regexp = "^\s*$|" + ProfileLinkPatterns.GITHUB, message = ProfileLinkPatterns.GITHUB_MESSAGE)
        String githubUrl,

        @Size(max = 200, message = "LeetCode profile URL must be at most 200 characters")
        @Pattern(regexp = "^\s*$|" + ProfileLinkPatterns.LEETCODE, message = ProfileLinkPatterns.LEETCODE_MESSAGE)
        String leetCodeUrl
) {
}
