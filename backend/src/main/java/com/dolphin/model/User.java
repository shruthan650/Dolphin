package com.dolphin.model;

import java.time.Instant;

public class User {

    private String id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private String githubUrl;
    private String leetCodeUrl;
    private boolean active;
    /** Consecutive wrong passwords since the last successful login. */
    private int failedLoginAttempts;
    /** Logins are rejected until this instant; null when the account is not locked. */
    private Instant lockedUntil;
    /** Incremented when the password or email changes; tokens carrying an older version are rejected. */
    private int tokenVersion;
    private Instant createdAt;
    private Instant updatedAt;

    public User copy() {
        User copy = new User();
        copy.id = id;
        copy.name = name;
        copy.email = email;
        copy.passwordHash = passwordHash;
        copy.role = role;
        copy.githubUrl = githubUrl;
        copy.leetCodeUrl = leetCodeUrl;
        copy.active = active;
        copy.failedLoginAttempts = failedLoginAttempts;
        copy.lockedUntil = lockedUntil;
        copy.tokenVersion = tokenVersion;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        return copy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLeetCodeUrl() { return leetCodeUrl; }
    public void setLeetCodeUrl(String leetCodeUrl) { this.leetCodeUrl = leetCodeUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    /** True while a lockout is in force at {@code now}. */
    public boolean isLockedAt(Instant now) { return lockedUntil != null && lockedUntil.isAfter(now); }

    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
