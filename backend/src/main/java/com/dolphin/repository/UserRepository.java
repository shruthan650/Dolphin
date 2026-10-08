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

    User save(User user);

    void deleteById(String id);

    long count();

    long countByRole(Role role);

    /**
     * Atomically records one wrong password, unless the account is locked at {@code now}. A lockout that has already
     * expired restarts the count at 1. When the count reaches {@code maxAttempts} the account is locked until
     * {@code lockUntil}. {@link #save} never touches these lockout fields.
     *
     * @return the new state, or empty when the account is currently locked (or does not exist)
     */
    Optional<LoginAttemptState> recordFailedLogin(String userId, Instant now, int maxAttempts, Instant lockUntil);

    /**
     * Atomically clears the failed-attempt count and lockout after a correct password, unless the account is locked
     * at {@code now} (e.g. a concurrent request has just locked it).
     *
     * @return false when the account is currently locked (or does not exist)
     */
    boolean resetFailedLogins(String userId, Instant now);

    /** Lockout state after a failed login. lockedUntil is null unless this attempt locked the account. */
    record LoginAttemptState(int failedAttempts, Instant lockedUntil) {
    }
}
