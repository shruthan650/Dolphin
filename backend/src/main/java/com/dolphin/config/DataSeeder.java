package com.dolphin.config;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import com.dolphin.validation.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Creates one initial admin only when explicitly configured. The durable bootstrap marker prevents recreating a
 * deleted admin account on later restarts.
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
                      @Value("${ADMIN_EMAIL:}") String adminEmail,
                      @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            userRepository.markAdminBootstrapCompleted();
            return;
        }
        if (userRepository.hasAdminBootstrapCompleted()) {
            log.warn("Initial admin is absent after bootstrap; refusing to recreate it");
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL and ADMIN_PASSWORD must be configured for initial admin creation");
        }
        if (!EmailAddress.isValid(adminEmail)) {
            throw new IllegalStateException("ADMIN_EMAIL must be a valid email address");
        }
        String normalizedEmail = EmailAddress.normalize(adminEmail);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("ADMIN_EMAIL is already assigned to a non-admin account");
        }
        Instant now = Instant.now();
        User admin = new User();
        admin.setName("Admin");
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        if (userRepository.createInitialAdmin(admin)) {
            log.info("Created initial administrator account");
        } else {
            log.info("Initial administrator was already initialized by another application instance");
        }
    }
}
