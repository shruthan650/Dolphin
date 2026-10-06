package com.dolphin.dto.teacher;

import com.dolphin.dto.common.ActivityResponse;

import java.util.List;

public record TeacherDashboardResponse(
        long totalClasses,
        long totalStudents,
        long totalProjects,
        long totalLeetCodeEntries,
        long problemsSolved,
        List<ActivityResponse> recentActivity
) {
}
