package com.dolphin.d1;

import com.dolphin.repository.d1.D1Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies the SQLite-backed D1 client when a test runs with dolphin.storage=d1 and dolphin.d1.client=sqlite. */
@Configuration
@ConditionalOnProperty(name = "dolphin.d1.client", havingValue = "sqlite")
public class SqliteD1TestConfig {

    @Bean
    public D1Client sqliteD1Client() {
        return new SqliteD1Client();
    }
}
