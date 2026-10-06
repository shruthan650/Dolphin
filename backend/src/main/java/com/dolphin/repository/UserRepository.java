package com.dolphin.repository;

import com.dolphin.model.Role;
import com.dolphin.model.User;

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
}
