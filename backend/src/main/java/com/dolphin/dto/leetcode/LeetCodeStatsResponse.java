package com.dolphin.dto.leetcode;

public record LeetCodeStatsResponse(
        long total,
        long solved,
        long attempted,
        long inProgress,
        long easySolved,
        long mediumSolved,
        long hardSolved
) {

    public static LeetCodeStatsResponse empty() {
        return new LeetCodeStatsResponse(0, 0, 0, 0, 0, 0, 0);
    }
}
