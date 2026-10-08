package com.dolphin.repository.d1;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dolphin.repository.d1.D1Values.bool;
import static com.dolphin.repository.d1.D1Values.enumValue;
import static com.dolphin.repository.d1.D1Values.instant;
import static com.dolphin.repository.d1.D1Values.number;
import static com.dolphin.repository.d1.D1Values.string;

@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "d1", matchIfMissing = true)
public class D1UserRepository implements UserRepository {

    private final D1Client d1;

    public D1UserRepository(D1Client d1) {
        this.d1 = d1;
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return d1.query("SELECT * FROM users WHERE id = ?", id).stream().findFirst().map(D1UserRepository::toUser);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return d1.query("SELECT * FROM users WHERE email_key = ?", normalize(email)).stream()
                .findFirst()
                .map(D1UserRepository::toUser);
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && !d1.query("SELECT 1 FROM users WHERE email_key = ?", normalize(email)).isEmpty();
    }

    @Override
    public List<User> findAll() {
        return d1.query("SELECT * FROM users ORDER BY created_at").stream().map(D1UserRepository::toUser).toList();
    }

    @Override
    public List<User> findByRole(Role role) {
        return d1.query("SELECT * FROM users WHERE role = ? ORDER BY created_at", role).stream()
                .map(D1UserRepository::toUser)
                .toList();
    }

    /** Keeps the order of {@code ids}, skipping unknown ones. */
    @Override
    public List<User> findAllById(Iterable<String> ids) {
        List<String> idList = new ArrayList<>(new LinkedHashSet<>(toList(ids)));
        Map<String, User> byId = D1Chunks.query(d1, "SELECT * FROM users WHERE id IN (%s)", idList).stream()
                .map(D1UserRepository::toUser)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<User> result = new ArrayList<>();
        for (String id : toList(ids)) {
            User user = byId.get(id);
            if (user != null) {
                result.add(user.copy());
            }
        }
        return result;
    }

    /**
     * The lockout columns are deliberately left out: they change only through the atomic methods below, so a
     * concurrent profile save can never reset a failed-attempt count.
     */
    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        d1.execute("""
                INSERT INTO users (id, name, email, email_key, password_hash, role, github_url, leetcode_url,
                                   active, token_version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = excluded.name, email = excluded.email, email_key = excluded.email_key,
                    password_hash = excluded.password_hash, role = excluded.role, github_url = excluded.github_url,
                    leetcode_url = excluded.leetcode_url, active = excluded.active,
                    token_version = excluded.token_version,
                    created_at = excluded.created_at, updated_at = excluded.updated_at
                """,
                user.getId(), user.getName(), user.getEmail(), normalize(user.getEmail()), user.getPasswordHash(),
                user.getRole(), user.getGithubUrl(), user.getLeetCodeUrl(), user.isActive(), user.getTokenVersion(),
                user.getCreatedAt(), user.getUpdatedAt());
        return user.copy();
    }

    /**
     * One statement, so concurrent wrong passwords are counted exactly; SET expressions see the old row values.
     * The limit is inlined because bound parameters arrive as text, which SQLite would not compare numerically here.
     */
    @Override
    public Optional<LoginAttemptState> recordFailedLogin(String userId, Instant now, int maxAttempts,
                                                         Instant lockUntil) {
        String newCount = "(CASE WHEN locked_until IS NOT NULL THEN 1 ELSE failed_login_attempts + 1 END)";
        return d1.query("""
                UPDATE users SET
                    failed_login_attempts = %1$s,
                    locked_until = CASE WHEN %1$s >= %2$d THEN ? ELSE NULL END
                WHERE id = ? AND (locked_until IS NULL OR locked_until <= ?)
                RETURNING failed_login_attempts, locked_until
                """.formatted(newCount, maxAttempts), lockUntil, userId, now).stream()
                .findFirst()
                .map(row -> new LoginAttemptState((int) number(row, "failed_login_attempts"),
                        instant(row, "locked_until")));
    }

    @Override
    public boolean resetFailedLogins(String userId, Instant now) {
        return !d1.query("""
                UPDATE users SET failed_login_attempts = 0, locked_until = NULL
                WHERE id = ? AND (locked_until IS NULL OR locked_until <= ?)
                RETURNING id
                """, userId, now).isEmpty();
    }

    @Override
    public void deleteById(String id) {
        d1.execute("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public long count() {
        return number(d1.query("SELECT COUNT(*) AS n FROM users").get(0), "n");
    }

    @Override
    public long countByRole(Role role) {
        return number(d1.query("SELECT COUNT(*) AS n FROM users WHERE role = ?", role).get(0), "n");
    }

    private static User toUser(Map<String, Object> row) {
        User user = new User();
        user.setId(string(row, "id"));
        user.setName(string(row, "name"));
        user.setEmail(string(row, "email"));
        user.setPasswordHash(string(row, "password_hash"));
        user.setRole(enumValue(row, "role", Role.class));
        user.setGithubUrl(string(row, "github_url"));
        user.setLeetCodeUrl(string(row, "leetcode_url"));
        user.setActive(bool(row, "active"));
        user.setFailedLoginAttempts((int) number(row, "failed_login_attempts"));
        user.setLockedUntil(instant(row, "locked_until"));
        user.setTokenVersion((int) number(row, "token_version"));
        user.setCreatedAt(instant(row, "created_at"));
        user.setUpdatedAt(instant(row, "updated_at"));
        return user;
    }

    private static List<String> toList(Iterable<String> ids) {
        List<String> list = new ArrayList<>();
        ids.forEach(list::add);
        return list;
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
