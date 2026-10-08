package com.dolphin.controller;

import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.project.ProjectResponse;
import com.dolphin.dto.teacher.TeacherDashboardResponse;
import com.dolphin.dto.teacher.TeacherStudentDetailResponse;
import com.dolphin.dto.teacher.TeacherStudentResponse;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.DashboardService;
import com.dolphin.service.TeacherService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final TeacherService teacherService;
    private final DashboardService dashboardService;

    public TeacherController(TeacherService teacherService, DashboardService dashboardService) {
        this.teacherService = teacherService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public TeacherDashboardResponse dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.teacherDashboard(user.id());
    }

    @GetMapping("/students")
    public List<TeacherStudentResponse> students(@AuthenticationPrincipal AuthenticatedUser user) {
        return teacherService.listStudents(user.id());
    }

    @GetMapping("/students/{studentId}")
    public TeacherStudentDetailResponse student(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable String studentId) {
        return teacherService.getStudent(user.id(), studentId);
    }

    @GetMapping("/students/{studentId}/projects")
    public List<ProjectResponse> studentProjects(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable String studentId) {
        return teacherService.getStudentProjects(user.id(), studentId);
    }

    @GetMapping("/students/{studentId}/leetcode")
    public List<LeetCodeResponse> studentLeetCode(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable String studentId) {
        return teacherService.getStudentLeetCode(user.id(), studentId);
    }

    @GetMapping("/projects")
    public List<ProjectResponse> projects(@AuthenticationPrincipal AuthenticatedUser user) {
        return teacherService.getAllStudentProjects(user.id());
    }

    @GetMapping("/leetcode")
    public List<LeetCodeResponse> leetCode(@AuthenticationPrincipal AuthenticatedUser user) {
        return teacherService.getAllStudentLeetCode(user.id());
    }
}
