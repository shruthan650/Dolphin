package com.dolphin.service;

import com.dolphin.dto.leetcode.DailySolveCount;
import com.dolphin.dto.leetcode.LeetCodeStreakResponse;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Service
public class LeetCodeStreakService {

    private static final int ACTIVITY_DAYS = 84;

    private final Clock clock;

    public LeetCodeStreakService(Clock clock) {
        this.clock = clock;
    }

    public LeetCodeStreakResponse calculate(List<LeetCodeEntry> entries) {
        LocalDate today = LocalDate.now(clock);
        NavigableMap<LocalDate, Integer> solvesByDate = new TreeMap<>();
        for (LeetCodeEntry entry : entries) {
            LocalDate solvedAt = entry.getSolvedAt();
            if (entry.getStatus() == LeetCodeStatus.SOLVED && solvedAt != null && !solvedAt.isAfter(today)) {
                solvesByDate.merge(solvedAt, 1, Integer::sum);
            }
        }

        LocalDate mostRecent = solvesByDate.isEmpty() ? null : solvesByDate.lastKey();
        int current = currentStreak(solvesByDate, today, mostRecent);
        int longest = longestStreak(solvesByDate);
        LocalDate activityStart = today.minusDays(ACTIVITY_DAYS - 1L);
        List<DailySolveCount> recentActivity = new ArrayList<>(ACTIVITY_DAYS);
        for (int day = 0; day < ACTIVITY_DAYS; day++) {
            LocalDate date = activityStart.plusDays(day);
            recentActivity.add(new DailySolveCount(date, solvesByDate.getOrDefault(date, 0)));
        }

        return new LeetCodeStreakResponse(current, longest, solvesByDate.size(), mostRecent, recentActivity);
    }

    private static int currentStreak(NavigableMap<LocalDate, Integer> solvesByDate, LocalDate today,
                                     LocalDate mostRecent) {
        if (mostRecent == null || mostRecent.isBefore(today.minusDays(1))) {
            return 0;
        }
        int streak = 0;
        for (LocalDate day = mostRecent; solvesByDate.containsKey(day); day = day.minusDays(1)) {
            streak++;
        }
        return streak;
    }

    private static int longestStreak(NavigableMap<LocalDate, Integer> solvesByDate) {
        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (Map.Entry<LocalDate, Integer> solve : solvesByDate.entrySet()) {
            LocalDate date = solve.getKey();
            current = previous != null && previous.plusDays(1).equals(date) ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = date;
        }
        return longest;
    }
}
