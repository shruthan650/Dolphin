package com.dolphin.dto.publicprofile;

public record PublicStudentSummaryResponse(
        String profileId,
        String displayName,
        long solvedProblems,
        int currentStreak,
        long publicProjectCount
) {
}
