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

/**
 * Creates the admin account from ADMIN_EMAIL / ADMIN_PASSWORD on startup when no admin exists yet (the first run
 * against an empty database). Once it exists the admin manages their own email and password from the profile page,
 * so they are never overwritten on restart. Teachers, classes, students and their data are intentionally NOT seeded.
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
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.warn("No admin account exists, but ADMIN_EMAIL {} belongs to another account; not seeding", adminEmail);
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
}
