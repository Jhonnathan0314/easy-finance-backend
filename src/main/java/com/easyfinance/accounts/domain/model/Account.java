package com.easyfinance.accounts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.Instant;
import java.util.Objects;

public final class Account {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final Long id;
    private final String name;
    private final String description;
    private final AccountStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Account(Long id, String name, String description, AccountStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Account create(String name, String description) {
        return new Account(null, name, description, AccountStatus.ACTIVE, null, null);
    }

    public static Account restore(Long id, String name, String description, AccountStatus status, Instant createdAt, Instant updatedAt) {
        return new Account(id, name, description, status, createdAt, updatedAt);
    }

    public Account update(String name, String description) {
        ensureWritable();
        return new Account(id, name, description, status, createdAt, updatedAt);
    }

    public Account archive() {
        ensureWritable();
        return new Account(id, name, description, AccountStatus.ARCHIVED, createdAt, updatedAt);
    }

    public void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException("ACCOUNT_NOT_ACTIVE", "Account is not active.");
        }
    }

    public void ensureWritable() {
        if (status == AccountStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("ACCOUNT_NOT_ACTIVE", "Account is not active.");
        }
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("ACCOUNT_NAME_REQUIRED", "Account name is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleViolationException("ACCOUNT_NAME_TOO_LONG", "Account name is too long.");
        }
        return normalized;
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleViolationException("ACCOUNT_DESCRIPTION_TOO_LONG", "Account description is too long.");
        }
        return normalized;
    }
}
