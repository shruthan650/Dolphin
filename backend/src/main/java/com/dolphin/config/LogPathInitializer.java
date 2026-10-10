package com.dolphin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LogPathInitializer {

    private static final Logger log = LoggerFactory.getLogger(LogPathInitializer.class);

    private LogPathInitializer() {
        // Utility class
    }

    public static void ensureExists() {
        String logPath = System.getenv().getOrDefault("LOG_PATH", "./logs");
        try {
            Path logsDirectory = Path.of(logPath);
            Files.createDirectories(logsDirectory);
            Files.createDirectories(logsDirectory.resolve("archive"));
        } catch (IOException ex) {
            log.warn("Could not create log directory {}: {}", logPath, ex.getMessage());
        }
    }
}
