package com.dolphin.dto.publicprofile;

import com.dolphin.dto.leetcode.DailySolveCount;

import java.time.LocalDate;
import java.util.List;

public record PublicLeetCodeStatsResponse(
        long totalSolved,
        long easySolved,
        long mediumSolved,
        long hardSolved,
        int currentStreak,
        int longestStreak,
        int activeDays,
        LocalDate lastSolvedDate,
        List<DailySolveCount> recentActivity
) {
}
