package com.dolphin;

import com.dolphin.config.LogPathInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Dolphin modular monolith. Authentication is JWT-based, so Spring Boot's
 * default
 * in-memory UserDetailsService (with its generated password) is disabled.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DolphinApplication {

    public static void main(String[] args) {
        LogPathInitializer.ensureExists();
        SpringApplication.run(DolphinApplication.class, args);
    }
}
