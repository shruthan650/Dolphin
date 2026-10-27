package com.dolphin;

import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;
import com.dolphin.service.LeetCodeStreakService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeetCodeStreakServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    private final LeetCodeStreakService service = new LeetCodeStreakService(
            Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void calculatesCurrentLongestAndRecentActivityFromDistinctSolvedDates() {
        var response = service.calculate(List.of(
                entry(LeetCodeStatus.SOLVED, TODAY),
                entry(LeetCodeStatus.SOLVED, TODAY),
                entry(LeetCodeStatus.SOLVED, TODAY.minusDays(1)),
                entry(LeetCodeStatus.SOLVED, TODAY.minusDays(2)),
                entry(LeetCodeStatus.SOLVED, TODAY.minusDays(4)),
                entry(LeetCodeStatus.ATTEMPTED, TODAY),
                entry(LeetCodeStatus.SOLVED, null),
                entry(LeetCodeStatus.SOLVED, TODAY.plusDays(1))));

        assertEquals(3, response.currentStreak());
        assertEquals(3, response.longestStreak());
        assertEquals(4, response.totalActiveDays());
        assertEquals(TODAY, response.mostRecentSolvedDate());
        assertEquals(84, response.recentActivity().size());
        assertEquals(2, response.recentActivity().get(83).solvedCount());
    }

    @Test
    void preservesYesterdayStreakButExpiresOlderStreak() {
        var yesterday = service.calculate(List.of(entry(LeetCodeStatus.SOLVED, TODAY.minusDays(1))));
        var expired = service.calculate(List.of(entry(LeetCodeStatus.SOLVED, TODAY.minusDays(2))));

        assertEquals(1, yesterday.currentStreak());
        assertEquals(0, expired.currentStreak());
        assertEquals(1, expired.longestStreak());
    }

    private static LeetCodeEntry entry(LeetCodeStatus status, LocalDate solvedAt) {
        LeetCodeEntry entry = new LeetCodeEntry();
        entry.setStatus(status);
        entry.setSolvedAt(solvedAt);
        return entry;
    }
}
