package com.dolphin.security;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    private User user() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("a@b.com");
        user.setRole(Role.TEACHER);
        return user;
    }

    @Test
    void generatedTokenRoundTrips() {
        JwtService.TokenClaims claims = jwtService.parse(jwtService.generateToken(user()));
        assertThat(claims.userId()).isEqualTo("user-1");
        assertThat(claims.email()).isEqualTo("a@b.com");
        assertThat(claims.role()).isEqualTo(Role.TEACHER);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.generateToken(user(), -1_000);
        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-key-0123456789", 60_000);
        String token = other.generateToken(user());
        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void missingOrShortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService("", 1000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService("short", 1000)).isInstanceOf(IllegalStateException.class);
    }
}
