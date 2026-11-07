package com.dolphin.repository;

import com.dolphin.model.Role;
import com.dolphin.model.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for users. Services depend only on this interface, so the
 * in-memory and Cloudflare D1 implementations are interchangeable (see {@code dolphin.storage}).
 */
public interface UserRepository {

    Optional<User> findById(String id);

    /** Email lookup is case-insensitive. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAll();

    List<User> findByRole(Role role);

    List<User> findAllById(Iterable<String> ids);

    List<PublicStudentCandidate> findPublicStudents(String search, String excludedUserId, int limit, int offset);

    long countPublicStudents(String search, String excludedUserId);

    Optional<PublicStudentCandidate> findPublicStudentByProfileId(String profileId);

    User save(User user);

    void deleteById(String id);

    long count();

    long countByRole(Role role);

    boolean hasAdminBootstrapCompleted();

    void markAdminBootstrapCompleted();

    boolean createInitialAdmin(User admin);

    boolean recordLoginAttempt(String keyHash, Instant windowStart, Instant expiredBefore, int maxAttempts);

    void resetLoginAttempts(String keyHash);

    /**
     * Atomically records one wrong password, unless the account is locked at {@code now}. A lockout that has already
     * expired restarts the count at 1. When the count reaches {@code maxAttempts} the account is locked until
     * {@code lockUntil}. {@link #save} never touches these lockout fields.
     *
     * @return the new state, or empty when the account is currently locked (or does not exist)
     */
    Optional<LoginAttemptState> recordFailedLogin(String userId, Instant now, int maxAttempts, Instant lockUntil);

    /** Clears legacy per-account lockout state; new login throttling uses the shared login-rate-limit store. */
    boolean resetFailedLogins(String userId, Instant now);

    /** Lockout state after a failed login. lockedUntil is null unless this attempt locked the account. */
    record LoginAttemptState(int failedAttempts, Instant lockedUntil) {
    }

    /** Restricted student fields needed internally to build allowlisted public responses. */
    record PublicStudentCandidate(String id, String profileId, String name, Instant joinedAt) {
    }
}
