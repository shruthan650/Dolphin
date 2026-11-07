package com.dolphin.dto.publicprofile;

import java.time.Instant;
import java.util.List;

public record PublicStudentProfileResponse(
        String profileId,
        String displayName,
        Instant joinedAt,
        PublicLeetCodeStatsResponse leetCodeStats,
        List<PublicStudentProjectResponse> projects
) {
}
