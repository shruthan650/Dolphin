package com.dolphin.dto.auth;

/** Shared validation rules for a student's GitHub and LeetCode profile URLs. */
public final class ProfileLinkPatterns {

    /** e.g. https://github.com/octocat */
    public static final String GITHUB = "^\\s*https?://(www\\.)?github\\.com/[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})/?\\s*$";
    public static final String GITHUB_MESSAGE = "Enter your GitHub profile URL, e.g. https://github.com/username";

    /** e.g. https://leetcode.com/u/username or https://leetcode.com/username */
    public static final String LEETCODE = "^\\s*https?://(www\\.)?leetcode\\.(com|cn)/(u/)?[A-Za-z0-9_.-]{1,50}/?\\s*$";
    public static final String LEETCODE_MESSAGE = "Enter your LeetCode profile URL, e.g. https://leetcode.com/u/username";

    private ProfileLinkPatterns() {
    }
}
