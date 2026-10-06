package com.dolphin.security;

import com.dolphin.model.Role;
import com.dolphin.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration:86400000}") long expirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable must be set to a value of at least "
                            + MIN_SECRET_BYTES + " characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        return generateToken(user, expirationMs);
    }

    /** Visible for tests that need an already-expired token (negative validity). */
    String generateToken(User user, long validityMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMs))
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies a token.
     *
     * @throws ExpiredJwtException if the token has expired
     * @throws JwtException        if the token is malformed or the signature is invalid
     */
    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new TokenClaims(
                claims.getSubject(),
                claims.get("email", String.class),
                Role.valueOf(claims.get("role", String.class)));
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public record TokenClaims(String userId, String email, Role role) {
    }
}
