package com.dolphin.dto.student;

import com.dolphin.dto.classdto.StudentClassResponse;
import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.leetcode.LeetCodeStatsResponse;
import com.dolphin.dto.project.ProjectResponse;

import java.util.List;

public record StudentDashboardResponse(
        List<StudentClassResponse> joinedClasses,
        long projectCount,
        LeetCodeStatsResponse leetCodeStats,
        List<ProjectResponse> recentProjects,
        List<LeetCodeResponse> recentLeetCode
) {
}
