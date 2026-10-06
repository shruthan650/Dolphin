package com.dolphin.controller;

import com.dolphin.dto.admin.AdminDashboardResponse;
import com.dolphin.dto.admin.AdminUserResponse;
import com.dolphin.dto.admin.CreateTeacherRequest;
import com.dolphin.dto.admin.UpdateUserStatusRequest;
import com.dolphin.service.AdminService;
import com.dolphin.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final DashboardService dashboardService;

    public AdminController(AdminService adminService, DashboardService dashboardService) {
        this.adminService = adminService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return dashboardService.adminDashboard();
    }

    @PostMapping("/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        return adminService.createTeacher(request);
    }

    @GetMapping("/teachers")
    public List<AdminUserResponse> teachers() {
        return adminService.listTeachers();
    }

    @PatchMapping("/teachers/{id}/status")
    public AdminUserResponse updateTeacherStatus(@PathVariable String id,
                                                 @Valid @RequestBody UpdateUserStatusRequest request) {
        return adminService.updateTeacherStatus(id, request.active());
    }

    @DeleteMapping("/teachers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacher(@PathVariable String id) {
        adminService.deleteTeacher(id);
    }

    @GetMapping("/students")
    public List<AdminUserResponse> students() {
        return adminService.listStudents();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return adminService.listUsers();
    }
}
