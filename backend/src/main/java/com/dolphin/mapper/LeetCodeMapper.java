package com.dolphin.mapper;

import com.dolphin.dto.leetcode.CreateLeetCodeRequest;
import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.leetcode.LeetCodeStatsResponse;
import com.dolphin.dto.leetcode.UpdateLeetCodeRequest;
import com.dolphin.model.Difficulty;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;

import static com.dolphin.mapper.MapperUtils.trimToNull;

public final class LeetCodeMapper {

    private LeetCodeMapper() {
    }

    public static LeetCodeEntry toEntity(CreateLeetCodeRequest request, String studentId, Instant now) {
        LeetCodeEntry entry = new LeetCodeEntry();
        entry.setStudentId(studentId);
        entry.setProblemName(request.problemName().trim());
        entry.setProblemUrl(trimToNull(request.problemUrl()));
        entry.setDifficulty(request.difficulty());
        entry.setStatus(request.status());
        entry.setTopic(trimToNull(request.topic()));
        entry.setSolvedAt(resolveSolvedAt(request.status(), request.solvedAt(), null, now));
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        return entry;
    }

    public static void applyUpdate(LeetCodeEntry entry, UpdateLeetCodeRequest request, Instant now) {
        entry.setProblemName(request.problemName().trim());
        entry.setProblemUrl(trimToNull(request.problemUrl()));
        entry.setDifficulty(request.difficulty());
        entry.setStatus(request.status());
        entry.setTopic(trimToNull(request.topic()));
        entry.setSolvedAt(resolveSolvedAt(request.status(), request.solvedAt(), entry.getSolvedAt(), now));
        entry.setUpdatedAt(now);
    }

    /** A solved problem always has a solved date (defaults to today); unsolved problems have none. */
    private static LocalDate resolveSolvedAt(LeetCodeStatus status, LocalDate requested, LocalDate existing,
                                             Instant now) {
        if (status != LeetCodeStatus.SOLVED) {
            return null;
        }
        if (requested != null) {
            return requested;
        }
        return existing != null ? existing : LocalDate.ofInstant(now, ZoneId.systemDefault());
    }

    public static LeetCodeResponse toResponse(LeetCodeEntry entry) {
        return toResponse(entry, null);
    }

    public static LeetCodeResponse toResponse(LeetCodeEntry entry, String studentName) {
        return new LeetCodeResponse(entry.getId(), entry.getStudentId(), studentName, entry.getProblemName(),
                entry.getProblemUrl(), entry.getDifficulty(), entry.getStatus(), entry.getTopic(),
                entry.getSolvedAt(), entry.getCreatedAt(), entry.getUpdatedAt());
    }

    public static LeetCodeStatsResponse toStats(Collection<LeetCodeEntry> entries) {
        long solved = 0, attempted = 0, inProgress = 0, easy = 0, medium = 0, hard = 0;
        for (LeetCodeEntry e : entries) {
            switch (e.getStatus()) {
                case SOLVED -> {
                    solved++;
                    if (e.getDifficulty() == Difficulty.EASY) easy++;
                    else if (e.getDifficulty() == Difficulty.MEDIUM) medium++;
                    else if (e.getDifficulty() == Difficulty.HARD) hard++;
                }
                case ATTEMPTED -> attempted++;
                case IN_PROGRESS -> inProgress++;
            }
        }
        return new LeetCodeStatsResponse(entries.size(), solved, attempted, inProgress, easy, medium, hard);
    }
}
