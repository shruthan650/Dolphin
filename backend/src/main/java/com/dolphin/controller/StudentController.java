package com.dolphin.controller;

import com.dolphin.dto.auth.UpdateProfileLinksRequest;
import com.dolphin.dto.classdto.StudentClassResponse;
import com.dolphin.dto.common.UserResponse;
import com.dolphin.dto.student.StudentDashboardResponse;
import com.dolphin.security.AuthenticatedUser;
import com.dolphin.service.AuthService;
import com.dolphin.service.ClassService;
import com.dolphin.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final DashboardService dashboardService;
    private final ClassService classService;
    private final AuthService authService;

    public StudentController(DashboardService dashboardService, ClassService classService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.classService = classService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public StudentDashboardResponse dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.studentDashboard(user.id());
    }

    @GetMapping("/classes")
    public List<StudentClassResponse> classes(@AuthenticationPrincipal AuthenticatedUser user) {
        return classService.listStudentClasses(user.id());
    }

    /** Leaves a class; the student's projects and LeetCode entries of that class are permanently deleted. */
    @DeleteMapping("/classes/{classId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveClass(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String classId) {
        classService.leaveClass(user.id(), classId);
    }

    @PutMapping("/profile-links")
    public UserResponse updateProfileLinks(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody UpdateProfileLinksRequest request) {
        return authService.updateProfileLinks(user.id(), request);
    }
}
