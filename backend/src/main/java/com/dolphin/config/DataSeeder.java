package com.dolphin.config;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Ensures the admin account exists on startup and that its password matches ADMIN_PASSWORD (creating the
 * account on the first run against an empty database).
 * Teachers, classes, students and their data are intentionally NOT seeded.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${dolphin.admin.email}") String adminEmail,
                      @Value("${dolphin.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<User> existing = userRepository.findByEmail(adminEmail);
        if (existing.isPresent()) {
            syncPassword(existing.get());
            return;
        }
        Instant now = Instant.now();
        User admin = new User();
        admin.setName("Admin");
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userRepository.save(admin);
        log.info("Seeded development admin account: {}", admin.getEmail());
    }

    /** ADMIN_PASSWORD is the source of truth: changing it and restarting changes the admin's password. */
    private void syncPassword(User admin) {
        if (passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
            return;
        }
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setUpdatedAt(Instant.now());
        userRepository.save(admin);
        log.info("Updated the admin password for {} from ADMIN_PASSWORD", admin.getEmail());
    }
}
