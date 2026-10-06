package com.dolphin.security;

import com.dolphin.model.User;

/** Test helper living in the security package so it can reach JwtService's package-private token builder. */
public final class JwtTestTokens {

    private JwtTestTokens() {
    }

    public static String expiredTokenFor(JwtService jwtService, User user) {
        return jwtService.generateToken(user, -60_000);
    }
}
