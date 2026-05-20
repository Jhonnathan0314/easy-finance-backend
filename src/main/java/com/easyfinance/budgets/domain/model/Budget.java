package com.easyfinance.budgets.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.Instant;

public final class Budget {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final Long id;
    private final Long accountId;
    private final Integer year;
    private final Integer month;
    private final String name;
    private final BudgetStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Budget(Long id, Long accountId, Integer year, Integer month, String name, BudgetStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountId = requireId(accountId, "BUDGET_ACCOUNT_REQUIRED", "Budget account is required.");
        this.year = requireYear(year);
        this.month = requireMonth(month);
        this.name = normalizeName(name);
        this.status = status == null ? BudgetStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Budget create(Long accountId, Integer year, Integer month, String name) {
        return new Budget(null, accountId, year, month, name, BudgetStatus.ACTIVE, null, null);
    }

    public static Budget restore(Long id, Long accountId, Integer year, Integer month, String name, BudgetStatus status, Instant createdAt, Instant updatedAt) {
        return new Budget(id, accountId, year, month, name, status, createdAt, updatedAt);
    }

    public Budget update(String name, BudgetStatus status) {
        return new Budget(id, accountId, year, month, name, status == null ? this.status : status, createdAt, updatedAt);
    }

    public void ensureActive() {
        if (status != BudgetStatus.ACTIVE) {
            throw new BusinessRuleViolationException("BUDGET_NOT_ACTIVE", "Budget is not active.");
        }
    }

    public Long id() { return id; }
    public Long accountId() { return accountId; }
    public Integer year() { return year; }
    public Integer month() { return month; }
    public String name() { return name; }
    public BudgetStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    static Long requireId(Long value, String code, String message) {
        if (value == null || value <= 0) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }

    static Integer requireYear(Integer value) {
        if (value == null || value < MIN_YEAR || value > MAX_YEAR) {
            throw new BusinessRuleViolationException("BUDGET_PERIOD_INVALID", "Budget year is invalid.");
        }
        return value;
    }

    static Integer requireMonth(Integer value) {
        if (value == null || value < 1 || value > 12) {
            throw new BusinessRuleViolationException("BUDGET_PERIOD_INVALID", "Budget month is invalid.");
        }
        return value;
    }

    static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 150) {
            throw new BusinessRuleViolationException("BUDGET_NAME_INVALID", "Budget name is too long.");
        }
        return trimmed;
    }
}
