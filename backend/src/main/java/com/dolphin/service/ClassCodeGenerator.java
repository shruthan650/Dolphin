package com.dolphin.service;

import com.dolphin.repository.ClassRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates unique, human-friendly class codes such as DOLPHIN-A8F2K. */
@Component
public class ClassCodeGenerator {

    /** No 0/O/1/I to avoid confusion when codes are read aloud or copied by hand. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String PREFIX = "DOLPHIN-";
    private static final int LENGTH = 5;
    private static final int MAX_ATTEMPTS = 50;

    private final SecureRandom random = new SecureRandom();
    private final ClassRepository classRepository;

    public ClassCodeGenerator(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!classRepository.existsByClassCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique class code");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
