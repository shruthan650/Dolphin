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
            log.info("Cloudflare D1 schema is up to date ({} statements)", statements.size());
        };
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
