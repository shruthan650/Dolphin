package com.dolphin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LogPathInitializer {

    private static final Logger log = LoggerFactory.getLogger(LogPathInitializer.class);

    public LogPathInitializer(@Value("${LOG_PATH:./logs}") String logPath) {
        try {
            Path logsDirectory = Path.of(logPath);
            Files.createDirectories(logsDirectory);
            Files.createDirectories(logsDirectory.resolve("archive"));
        } catch (IOException ex) {
            log.warn("Could not create log directory {}: {}", logPath, ex.getMessage());
        }
    }
}
