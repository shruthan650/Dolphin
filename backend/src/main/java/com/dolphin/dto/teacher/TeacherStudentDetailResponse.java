package com.dolphin.dto.teacher;

import com.dolphin.dto.advice.AdviceResponse;
import com.dolphin.dto.classdto.ClassResponse;
import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.leetcode.LeetCodeStatsResponse;
import com.dolphin.dto.project.ProjectResponse;

import java.time.Instant;
import java.util.List;

public record TeacherStudentDetailResponse(
        String id,
        String name,
        String email,
        String githubUrl,
        String leetCodeUrl,
        boolean active,
        Instant joinedAt,
        List<ClassResponse> classes,
        LeetCodeStatsResponse leetCodeStats,
        List<ProjectResponse> projects,
        List<LeetCodeResponse> leetCodeEntries,
        List<AdviceResponse> advice
) {
}
