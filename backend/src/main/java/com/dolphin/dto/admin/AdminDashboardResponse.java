package com.dolphin.dto.admin;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalTeachers,
        long activeTeachers,
        long totalStudents,
        long totalClasses,
        long totalProjects,
        long totalLeetCodeEntries,
        long problemsSolved,
        List<AdminUserResponse> recentUsers
) {
}
