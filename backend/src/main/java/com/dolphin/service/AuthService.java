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
import com.dolphin.repository.UserRepository.LoginAttemptState;
import com.dolphin.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String LOCKED = "Too many login attempts. Please try again later.";
    /** Consecutive wrong passwords that lock an account... */
    static final int MAX_FAILED_ATTEMPTS = 3;
    /** ...and for how long. It unlocks by itself afterwards. */
    static final Duration LOCKOUT_DURATION = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    /** Used to keep login timing similar whether or not the email exists. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dolphin-dummy-password");
    }

    /**
     * Email + password login with a persisted lockout: {@value #MAX_FAILED_ATTEMPTS} consecutive wrong passwords lock
     * the account for {@link #LOCKOUT_DURATION}, during which even the correct password is rejected. The counter and
     * lock live in the database and change through atomic updates, so concurrent requests or several backend
     * instances cannot get extra attempts; only a successful login resets them.
     */
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(request.email());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        User user = found.get();
        Instant now = clock.instant();
        if (user.isLockedAt(now)) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new TooManyAttemptsException(LOCKED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            Optional<LoginAttemptState> state = userRepository.recordFailedLogin(user.getId(), now,
                    MAX_FAILED_ATTEMPTS, now.plus(LOCKOUT_DURATION));
            if (state.isEmpty() || state.get().lockedUntil() != null) {
                throw new TooManyAttemptsException(LOCKED);
            }
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        // Fails if a concurrent request locked the account after it was read above.
        if (!userRepository.resetFailedLogins(user.getId(), now)) {
            throw new TooManyAttemptsException(LOCKED);
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("This account has been deactivated. Contact an administrator.");
        }
        return toLoginResponse(user);
    }

    /** Public self-registration; always creates a STUDENT. */
    public LoginResponse registerStudent(RegisterStudentRequest request) {
        User student = createUser(request.name(), request.email(), request.password(), request.confirmPassword(),
                Role.STUDENT, user -> applyProfileLinks(user, request.githubUrl(), request.leetCodeUrl()));
        return toLoginResponse(student);
    }

    /** Adds or replaces a student's GitHub and LeetCode profile URLs. */
    public UserResponse updateProfileLinks(String userId, UpdateProfileLinksRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        applyProfileLinks(user, request.githubUrl(), request.leetCodeUrl());
        user.setUpdatedAt(Instant.now());
        return UserMapper.toResponse(userRepository.save(user));
    }

    public UserResponse currentUser(String userId) {
        return userRepository.findById(userId)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** Shared by student registration and admin teacher creation. */
    User createUser(String name, String email, String password, String confirmPassword, Role role) {
        return createUser(name, email, password, confirmPassword, role, user -> { });
    }

    private User createUser(String name, String email, String password, String confirmPassword, Role role,
                            Consumer<User> customizer) {
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
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
        return userRepository.save(user);
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
