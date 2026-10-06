package com.dolphin.security;

import com.dolphin.model.Role;

/** The principal stored in the SecurityContext for an authenticated request. */
public record AuthenticatedUser(String id, String email, String name, Role role) {
}
