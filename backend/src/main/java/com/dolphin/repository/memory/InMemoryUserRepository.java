package com.dolphin.repository.memory;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory user store. Data is lost when the application stops.
 * Stored objects are copied on the way in and out so callers can never mutate state without save().
 */
@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "memory")
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> idByEmail = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(id)).map(User::copy);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(idByEmail.get(normalize(email))).flatMap(this::findById);
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && idByEmail.containsKey(normalize(email));
    }

    @Override
    public List<User> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(User::copy)
                .toList();
    }

    @Override
    public List<User> findByRole(Role role) {
        return findAll().stream().filter(u -> u.getRole() == role).toList();
    }

    @Override
    public List<User> findAllById(Iterable<String> ids) {
        List<User> result = new ArrayList<>();
        for (String id : ids) {
            findById(id).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public synchronized User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        User previous = users.get(user.getId());
        if (previous != null && previous.getEmail() != null) {
            idByEmail.remove(normalize(previous.getEmail()));
        }
        User stored = user.copy();
        users.put(stored.getId(), stored);
        if (stored.getEmail() != null) {
            idByEmail.put(normalize(stored.getEmail()), stored.getId());
        }
        return stored.copy();
    }

    @Override
    public synchronized void deleteById(String id) {
        User removed = users.remove(id);
        if (removed != null && removed.getEmail() != null) {
            idByEmail.remove(normalize(removed.getEmail()));
        }
    }

    @Override
    public long count() {
        return users.size();
    }

    @Override
    public long countByRole(Role role) {
        return users.values().stream().filter(u -> u.getRole() == role).count();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
