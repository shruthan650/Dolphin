package com.dolphin.config;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.memory.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataSeederTest {

    @Test
    void requiresExplicitCredentialsBeforeFirstAdminBootstrap() {
        DataSeeder seeder = seeder(new InMemoryUserRepository(), "", "");

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL and ADMIN_PASSWORD");
    }

    @Test
    void createsInitialAdminOnceAndDoesNotRecreateItAfterDeletion() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        DataSeeder seeder = seeder(users, "Admin@Example.com", "Strong-test-password-123");

        seeder.run(null);

        User created = users.findByEmail("admin@example.com").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.ADMIN);
        assertThat(created.getPasswordHash()).isNotEqualTo("Strong-test-password-123");
        users.deleteById(created.getId());

        seeder.run(null);

        assertThat(users.findByEmail("admin@example.com")).isEmpty();
        assertThat(users.countByRole(Role.ADMIN)).isZero();
    }

    @Test
    void preservesAnExistingAdminInsteadOfReplacingCredentials() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        User existing = new User();
        existing.setName("Existing");
        existing.setEmail("admin@example.com");
        existing.setPasswordHash("existing-hash");
        existing.setRole(Role.ADMIN);
        existing.setActive(true);
        existing.setCreatedAt(Instant.now());
        users.save(existing);
        DataSeeder seeder = seeder(users, "replacement@example.com", "Strong-test-password-123");

        seeder.run(null);

        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(users.findByEmail("admin@example.com").orElseThrow().getPasswordHash())
                .isEqualTo("existing-hash");
        assertThat(users.findByEmail("replacement@example.com")).isEmpty();
    }

    private static DataSeeder seeder(InMemoryUserRepository users, String email, String password) {
        return new DataSeeder(users, new BCryptPasswordEncoder(), email, password);
    }
}
