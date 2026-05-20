package com.easyfinance.identity.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class User {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UserStatus status;
    private final Set<GlobalRoleName> globalRoles;

    private User(Long id, String email, String passwordHash, String fullName, UserStatus status, Set<GlobalRoleName> globalRoles) {
        this.id = id;
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "PASSWORD_HASH_REQUIRED", "Password hash is required.");
        this.fullName = requireText(fullName, "FULL_NAME_REQUIRED", "Full name is required.");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.globalRoles = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(globalRoles, "globalRoles must not be null")));
    }

    public static User register(String email, String passwordHash, String fullName) {
        return new User(null, email, passwordHash, fullName, UserStatus.ACTIVE, Set.of(GlobalRoleName.USER));
    }

    public static User restore(Long id, String email, String passwordHash, String fullName, UserStatus status, Set<GlobalRoleName> globalRoles) {
        return new User(id, email, passwordHash, fullName, status, globalRoles);
    }

    public void ensureCanLogin() {
        if (status == UserStatus.BLOCKED) {
            throw new BusinessRuleViolationException("USER_BLOCKED", "User cannot login.");
        }
        if (status == UserStatus.INACTIVE) {
            throw new BusinessRuleViolationException("USER_INACTIVE", "User cannot login.");
        }
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String fullName() {
        return fullName;
    }

    public UserStatus status() {
        return status;
    }

    public Set<GlobalRoleName> globalRoles() {
        return globalRoles;
    }

    private static String normalizeEmail(String value) {
        String email = requireText(value, "EMAIL_REQUIRED", "Email is required.").toLowerCase(Locale.ROOT);
        if (!email.contains("@")) {
            throw new BusinessRuleViolationException("EMAIL_INVALID", "Email is invalid.");
        }
        return email;
    }

    private static String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value.trim();
    }
}

