package com.dolphin.service;

import com.dolphin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/** Persistent, shared login throttling; only keyed hashes of client identifiers are stored. */
@Service
public class LoginRateLimiter {

    private final UserRepository userRepository;
    private final Clock clock;
    private final byte[] key;
    private final Duration window;
    private final int ipLimit;
    private final int accountIpLimit;

    public LoginRateLimiter(
            UserRepository userRepository,
            Clock clock,
            @Value("${jwt.secret}") String secret,
            @Value("${login.rate-limit.window:15m}") Duration window,
            @Value("${login.rate-limit.ip-max-attempts:120}") int ipLimit,
            @Value("${login.rate-limit.account-ip-max-attempts:8}") int accountIpLimit) {
        if (window.isZero() || window.isNegative() || window.toSeconds() < 1
                || ipLimit < 1 || accountIpLimit < 1) {
            throw new IllegalArgumentException("Login rate-limit configuration must use positive values");
        }
        this.userRepository = userRepository;
        this.clock = clock;
        this.key = secret.getBytes(StandardCharsets.UTF_8);
        this.window = window;
        this.ipLimit = ipLimit;
        this.accountIpLimit = accountIpLimit;
    }

    public boolean allow(String email, String remoteAddress) {
        Instant now = clock.instant();
        Instant windowStart = windowStart(now);
        Instant expiredBefore = now.minus(window);
        String ipHash = hash("ip:" + safeAddress(remoteAddress));
        String accountIpHash = hash("account:" + email + ":" + safeAddress(remoteAddress));
        boolean withinIpLimit = userRepository.recordLoginAttempt(ipHash, windowStart, expiredBefore, ipLimit);
        boolean withinAccountLimit = userRepository.recordLoginAttempt(
                accountIpHash, windowStart, expiredBefore, accountIpLimit);
        return withinIpLimit && withinAccountLimit;
    }

    public void resetAccountIp(String email, String remoteAddress) {
        userRepository.resetLoginAttempts(hash("account:" + email + ":" + safeAddress(remoteAddress)));
    }

    private Instant windowStart(Instant now) {
        long windowSeconds = window.toSeconds();
        return Instant.ofEpochSecond(Math.floorDiv(now.getEpochSecond(), windowSeconds) * windowSeconds);
    }

    private static String safeAddress(String address) {
        return address == null || address.isBlank() ? "unknown" : address;
    }

    private String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to initialize login rate limiting", ex);
        }
    }
}
