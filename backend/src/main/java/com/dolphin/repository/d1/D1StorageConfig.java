package com.dolphin.repository.d1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wires Cloudflare D1 storage when {@code dolphin.storage=d1}. The schema is applied on startup, before the
 * admin seeder runs, so a brand-new empty D1 database works without any manual setup.
 */
@Configuration
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "d1", matchIfMissing = true)
public class D1StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(D1StorageConfig.class);

    /** dolphin.d1.client=cloudflare (default) talks to the real D1 API; tests plug in a local SQLite client. */
    @Bean
    @ConditionalOnProperty(name = "dolphin.d1.client", havingValue = "cloudflare", matchIfMissing = true)
    public D1Client cloudflareD1Client(@Value("${dolphin.d1.account-id:}") String accountId,
                                       @Value("${dolphin.d1.database-id:}") String databaseId,
                                       @Value("${dolphin.d1.api-token:}") String apiToken,
                                       @Value("${dolphin.d1.timeout:15s}") Duration timeout,
                                       ObjectMapper objectMapper) {
        return new CloudflareD1Client(accountId, databaseId, apiToken, timeout, objectMapper);
    }

    @Bean
    public InitializingBean d1SchemaInitializer(D1Client d1Client) {
        return () -> {
            List<D1Client.D1Statement> statements = loadSchema().stream()
                    .map(sql -> new D1Client.D1Statement(sql, List.of()))
                    .toList();
            d1Client.batch(statements);
            migrate(d1Client);
            log.info("Cloudflare D1 schema is up to date ({} statements)", statements.size());
        };
    }

    /** A column added after the first release: added to existing tables when missing, then {@code afterAdd} runs. */
    record ColumnMigration(String table, String column, String definition, List<String> afterAdd) {
    }

    /**
     * Columns added after the first release. Brand-new databases already get them from schema.sql; older ones get them
     * here, exactly once (SQLite's ADD COLUMN is not idempotent, so the column list is checked first).
     */
    static final List<ColumnMigration> MIGRATIONS = List.of(
            new ColumnMigration("users", "failed_login_attempts", "INTEGER NOT NULL DEFAULT 0", List.of()),
            new ColumnMigration("users", "locked_until", "TEXT", List.of()),
            new ColumnMigration("users", "token_version", "INTEGER NOT NULL DEFAULT 0", List.of()),
            // Existing projects/entries are assigned to the student's class when they are in exactly one class;
            // otherwise ownership cannot be determined and they stay unassigned (never deleted with a class).
            new ColumnMigration("projects", "class_id", "TEXT", List.of("""
                    UPDATE projects SET class_id = (
                        SELECT s.class_id FROM class_students s WHERE s.student_id = projects.owner_id)
                    WHERE class_id IS NULL
                      AND (SELECT COUNT(*) FROM class_students s WHERE s.student_id = projects.owner_id) = 1
                    """)),
            new ColumnMigration("leetcode_entries", "class_id", "TEXT", List.of("""
                    UPDATE leetcode_entries SET class_id = (
                        SELECT s.class_id FROM class_students s WHERE s.student_id = leetcode_entries.student_id)
                    WHERE class_id IS NULL
                      AND (SELECT COUNT(*) FROM class_students s
                           WHERE s.student_id = leetcode_entries.student_id) = 1
                    """)));

    /** Indexes on migrated columns; they can only be created once the columns exist. */
    static final List<String> MIGRATED_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_projects_class ON projects (class_id)",
            "CREATE INDEX IF NOT EXISTS idx_leetcode_class ON leetcode_entries (class_id)");

    static void migrate(D1Client d1Client) {
        for (ColumnMigration m : MIGRATIONS) {
            boolean present = d1Client.query("PRAGMA table_info(" + m.table() + ")").stream()
                    .anyMatch(row -> m.column().equals(String.valueOf(row.get("name"))));
            if (present) {
                continue;
            }
            List<D1Client.D1Statement> statements = new ArrayList<>();
            statements.add(new D1Client.D1Statement(
                    "ALTER TABLE " + m.table() + " ADD COLUMN " + m.column() + " " + m.definition(), List.of()));
            m.afterAdd().forEach(sql -> statements.add(new D1Client.D1Statement(sql, List.of())));
            d1Client.batch(statements);
            log.info("Cloudflare D1 migration: added {}.{}", m.table(), m.column());
        }
        d1Client.batch(MIGRATED_INDEXES.stream().map(sql -> new D1Client.D1Statement(sql, List.of())).toList());
    }

    static List<String> loadSchema() throws IOException {
        String script = new ClassPathResource("d1/schema.sql").getContentAsString(StandardCharsets.UTF_8);
        String withoutComments = script.replaceAll("--[^\\n]*", "");
        return Arrays.stream(withoutComments.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
