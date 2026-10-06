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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminService {

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
        return UserMapper.toAdminResponse(saved, classCount(saved));
    }

    /** Permanently deletes a teacher and their classes; enrolled students keep their accounts. */
    public void deleteTeacher(String teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("Only teacher accounts can be deleted here");
        }
        accountDeletionService.deleteTeacher(teacherId);
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
