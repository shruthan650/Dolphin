package com.dolphin.service;

import com.dolphin.dto.admin.AdminUserResponse;
import com.dolphin.dto.admin.CreateTeacherRequest;
import com.dolphin.exception.BadRequestException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.mapper.UserMapper;
import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.ClassRepository;
import com.dolphin.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final AuthService authService;
    private final AccountDeletionService accountDeletionService;

    public AdminService(UserRepository userRepository, ClassRepository classRepository, AuthService authService,
            AccountDeletionService accountDeletionService) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.authService = authService;
        this.accountDeletionService = accountDeletionService;
    }

    public AdminUserResponse createTeacher(CreateTeacherRequest request) {
        User teacher = authService.createUser(request.name(), request.email(), request.password(),
                request.confirmPassword(), Role.TEACHER);
        log.info("Admin created teacher account {}", teacher.getId());
        return UserMapper.toAdminResponse(teacher, 0);
    }

    public List<AdminUserResponse> listTeachers() {
        return toAdminResponses(userRepository.findByRole(Role.TEACHER));
    }

    public List<AdminUserResponse> listStudents() {
        return toAdminResponses(userRepository.findByRole(Role.STUDENT));
    }

    public List<AdminUserResponse> listUsers() {
        return toAdminResponses(userRepository.findAll());
    }

    public AdminUserResponse updateTeacherStatus(String teacherId, boolean active) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("Only teacher accounts can be activated or deactivated here");
        }
        teacher.setActive(active);
        teacher.setUpdatedAt(Instant.now());
        User saved = userRepository.save(teacher);
        log.info("Admin {} teacher account {}", active ? "activated" : "deactivated", teacherId);
        return UserMapper.toAdminResponse(saved, classCount(saved));
    }

    /**
     * Permanently deletes a teacher and their classes; enrolled students keep their
     * accounts and their projects and
     * LeetCode entries of those classes (which become unassigned).
     */
    public void deleteTeacher(String teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("Only teacher accounts can be deleted here");
        }
        accountDeletionService.deleteTeacher(teacherId);
        log.info("Admin deleted teacher account {}", teacherId);
    }

    /**
     * Permanently deletes a student with all their data and every class enrolment.
     */
    public void deleteStudent(String studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only student accounts can be deleted here");
        }
        accountDeletionService.deleteStudent(studentId);
        log.info("Admin deleted student account {}", studentId);
    }

    List<AdminUserResponse> toAdminResponses(List<User> users) {
        return users.stream().map(u -> UserMapper.toAdminResponse(u, classCount(u))).toList();
    }

    private int classCount(User user) {
        return switch (user.getRole()) {
            case TEACHER -> classRepository.findByTeacherId(user.getId()).size();
            case STUDENT -> classRepository.findByStudentId(user.getId()).size();
            case ADMIN -> 0;
        };
    }
}
