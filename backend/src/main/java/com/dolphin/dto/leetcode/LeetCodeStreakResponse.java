package com.dolphin.dto.leetcode;

import java.time.LocalDate;
import java.util.List;

public record LeetCodeStreakResponse(
        int currentStreak,
        int longestStreak,
        int totalActiveDays,
        LocalDate mostRecentSolvedDate,
        List<DailySolveCount> recentActivity
) {
}
