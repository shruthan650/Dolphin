package com.dolphin.service;

import com.dolphin.dto.auth.LoginRequest;
import com.dolphin.dto.auth.LoginResponse;
import com.dolphin.dto.auth.RegisterStudentRequest;
import com.dolphin.dto.auth.UpdateProfileLinksRequest;
import com.dolphin.dto.common.UserResponse;
import com.dolphin.exception.BadRequestException;
import com.dolphin.exception.ConflictException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.exception.TooManyAttemptsException;
import com.dolphin.exception.UnauthorizedException;
import com.dolphin.mapper.UserMapper;
import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import com.dolphin.validation.EmailAddress;
import com.dolphin.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final LoginRateLimiter loginRateLimiter;
    /** Used to keep login timing similar whether or not the email exists. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            Clock clock, LoginRateLimiter loginRateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.loginRateLimiter = loginRateLimiter;
        this.dummyHash = passwordEncoder.encode("dolphin-dummy-password");
    }

    /** Uses shared IP and account/IP throttles. Forwarded IP headers are deliberately ignored. */
    public LoginResponse login(LoginRequest request, String remoteAddress) {
        if (!EmailAddress.isValid(request.email())) {
            throw new BadRequestException("Email must be valid");
        }
        String email = EmailAddress.normalize(request.email());
        if (!loginRateLimiter.allow(email, remoteAddress)) {
            log.warn("Login request throttled");
            throw new TooManyAttemptsException(INVALID_CREDENTIALS);
        }
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            log.warn("Authentication failed");
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Authentication failed");
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            log.warn("Authentication denied for inactive account");
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        Instant now = clock.instant();
        userRepository.resetFailedLogins(user.getId(), now);
        loginRateLimiter.resetAccountIp(email, remoteAddress);
        log.info("User authenticated successfully");
        return toLoginResponse(user);
    }

    /** Public self-registration; always creates a STUDENT. */
    public LoginResponse registerStudent(RegisterStudentRequest request) {
        User student = createUser(request.name(), request.email(), request.password(), request.confirmPassword(),
                Role.STUDENT, user -> applyProfileLinks(user, request.githubUrl(), request.leetCodeUrl()));
        log.info("Student account created: {}", student.getId());
        return toLoginResponse(student);
    }

    /** Adds or replaces a student's GitHub and LeetCode profile URLs. */
    public UserResponse updateProfileLinks(String userId, UpdateProfileLinksRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        applyProfileLinks(user, request.githubUrl(), request.leetCodeUrl());
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("Updated profile links for user {}", userId);
        return UserMapper.toResponse(saved);
    }

    public UserResponse currentUser(String userId) {
        return userRepository.findById(userId)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** Shared by student registration and admin teacher creation. */
    User createUser(String name, String email, String password, String confirmPassword, Role role) {
        return createUser(name, email, password, confirmPassword, role, user -> {
        });
    }

    private User createUser(String name, String email, String password, String confirmPassword, Role role,
            Consumer<User> customizer) {
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
        if (!EmailAddress.isValid(email)) {
            throw new BadRequestException("Email must be valid");
        }
        String normalizedEmail = EmailAddress.normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }
        Instant now = Instant.now();
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        customizer.accept(user);
        User saved = userRepository.save(user);
        log.info("Created {} account {}", role, saved.getId());
        return saved;
    }

    LoginResponse toLoginResponse(User user) {
        return new LoginResponse(jwtService.generateToken(user), user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.getGithubUrl(), user.getLeetCodeUrl(), jwtService.getExpirationMs());
    }

    private static void applyProfileLinks(User user, String githubUrl, String leetCodeUrl) {
        user.setGithubUrl(normalizeUrl(githubUrl));
        user.setLeetCodeUrl(normalizeUrl(leetCodeUrl));
    }

    /** Trims whitespace and a trailing slash so the stored URL is canonical. */
    static String normalizeUrl(String url) {
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
