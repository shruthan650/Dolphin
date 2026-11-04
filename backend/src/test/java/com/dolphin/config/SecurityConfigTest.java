package com.dolphin.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    @Test
    void corsAllowsOnlyConfiguredExactOrigins() {
        CorsConfigurationSource source = new SecurityConfig(null, null, null)
                .corsConfigurationSource("https://dolphin.example, https://dolphin-preview.example");
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config.getAllowedOrigins())
                .containsExactly("https://dolphin.example", "https://dolphin-preview.example");
        assertThat(config.checkOrigin("https://dolphin.example")).isEqualTo("https://dolphin.example");
        assertThat(config.checkOrigin("https://attacker.example")).isNull();
    }

    @Test
    void corsRejectsWildcardOrigins() {
        assertThatThrownBy(() -> new SecurityConfig(null, null, null)
                .corsConfigurationSource("https://dolphin-*.vercel.app"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exact origins");
    }
}
