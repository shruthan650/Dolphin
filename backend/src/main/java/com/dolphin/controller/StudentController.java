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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PutMapping("/profile-links")
    public UserResponse updateProfileLinks(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody UpdateProfileLinksRequest request) {
        return authService.updateProfileLinks(user.id(), request);
    }
}
