package com.dolphin.service;

import com.dolphin.dto.account.ChangeEmailRequest;
import com.dolphin.dto.account.ChangePasswordRequest;
import com.dolphin.dto.account.DeleteAccountRequest;
import com.dolphin.dto.account.UpdateProfileRequest;
import com.dolphin.dto.auth.LoginResponse;
import com.dolphin.dto.common.UserResponse;
import com.dolphin.exception.BadRequestException;
import com.dolphin.exception.ConflictException;
import com.dolphin.exception.ForbiddenException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.mapper.UserMapper;
import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

/**
 * Self-service account management. Every method acts on the authenticated
 * user's own id (taken from the JWT by the
 * controller), so one user can never change another user's account through
 * these operations.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final String DELETE_CONFIRMATION = "DELETE";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AccountDeletionService accountDeletionService;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService,
            AccountDeletionService accountDeletionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.accountDeletionService = accountDeletionService;
    }

    public UserResponse get(String userId) {
        return UserMapper.toResponse(require(userId));
    }

    /**
     * Name for everyone; GitHub/LeetCode links are required for students and
     * optional for other roles.
     */
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = require(userId);
        boolean blankGithub = request.githubUrl() == null || request.githubUrl().isBlank();
        boolean blankLeetCode = request.leetCodeUrl() == null || request.leetCodeUrl().isBlank();
        if (user.getRole() == Role.STUDENT && (blankGithub || blankLeetCode)) {
            throw new BadRequestException(blankGithub ? "GitHub profile URL is required"
                    : "LeetCode profile URL is required");
        }
        user.setName(request.name().trim());
        user.setGithubUrl(blankGithub ? null : AuthService.normalizeUrl(request.githubUrl()));
        user.setLeetCodeUrl(blankLeetCode ? null : AuthService.normalizeUrl(request.leetCodeUrl()));
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("User {} updated profile details", userId);
        return UserMapper.toResponse(saved);
    }

    /**
     * Changes the login email after re-checking the password. Older tokens are
     * revoked (they carry the old email)
     * and a fresh token is returned so the current session continues.
     */
    public LoginResponse changeEmail(String userId, ChangeEmailRequest request) {
        User user = require(userId);
        verifyPassword(user, request.currentPassword());
        String newEmail = request.newEmail().trim().toLowerCase(Locale.ROOT);
        if (newEmail.equals(user.getEmail())) {
            throw new BadRequestException("This is already your email address");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("An account with this email already exists");
        }
        user.setEmail(newEmail);
        log.info("User {} changed email address", userId);
        return saveWithNewTokenVersion(user);
    }

    /**
     * Re-hashes with the application's PasswordEncoder (BCrypt) and revokes every
     * other session.
     */
    public LoginResponse changePassword(String userId, ChangePasswordRequest request) {
        User user = require(userId);
        verifyPassword(user, request.currentPassword());
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("The new password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("User {} changed password and revoked older sessions", userId);
        return saveWithNewTokenVersion(user);
    }

    /**
     * Students and teachers may delete their own account; admins may not (the
     * system needs its administrator).
     */
    public void deleteOwnAccount(String userId, DeleteAccountRequest request) {
        User user = require(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Administrator accounts cannot be deleted");
        }
        if (!DELETE_CONFIRMATION.equals(request.confirmation().trim())) {
            throw new BadRequestException("Type DELETE to confirm");
        }
        verifyPassword(user, request.currentPassword());
        if (user.getRole() == Role.TEACHER) {
            accountDeletionService.deleteTeacher(userId);
        } else {
            accountDeletionService.deleteStudent(userId);
        }
        log.info("User {} deleted their own account", userId);
    }

    private LoginResponse saveWithNewTokenVersion(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setUpdatedAt(Instant.now());
        return authService.toLoginResponse(userRepository.save(user));
    }

    /**
     * 400 rather than 401, so a typo does not look like an expired session to the
     * frontend.
     */
    private void verifyPassword(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
    }

    private User require(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
