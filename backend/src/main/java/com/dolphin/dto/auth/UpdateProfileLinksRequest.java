package com.dolphin.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lets a student add or change their GitHub and LeetCode profile URLs. */
public record UpdateProfileLinksRequest(
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
