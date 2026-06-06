package com.easyfinance.income.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class Income {

    private final Long id;
    private final Long accountId;
    private final Long categoryId;
    private final Long participantId;
    private final String description;
    private final Money amount;
    private final LocalDate incomeDate;
    private final IncomeStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Income(Long id, Long accountId, Long categoryId, Long participantId, String description, Money amount, LocalDate incomeDate, IncomeStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountId = requireId(accountId, "INCOME_ACCOUNT_REQUIRED", "Account id is required.");
        this.categoryId = requireId(categoryId, "INCOME_CATEGORY_REQUIRED", "Income category is required.");
        this.participantId = requireId(participantId, "INCOME_PARTICIPANT_REQUIRED", "Participant id is required.");
        this.description = IncomeText.normalizeDescription(description);
        this.amount = requirePositiveAmount(amount);
        this.incomeDate = requireIncomeDate(incomeDate);
        this.status = requireStatus(status);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Income create(Long accountId, Long categoryId, Long participantId, String description, Money amount, LocalDate incomeDate) {
        return new Income(null, accountId, categoryId, participantId, description, amount, incomeDate, IncomeStatus.ACTIVE, null, null);
    }

    public static Income restore(Long id, Long accountId, Long categoryId, Long participantId, String description, Money amount, LocalDate incomeDate, IncomeStatus status, Instant createdAt, Instant updatedAt) {
        return new Income(id, accountId, categoryId, participantId, description, amount, incomeDate, status, createdAt, updatedAt);
    }

    public Income update(Long categoryId, Long participantId, String description, Money amount, LocalDate incomeDate) {
        ensureActive();
        return new Income(id, accountId, categoryId, participantId, description, amount, incomeDate, status, createdAt, updatedAt);
    }

    public Income cancel() {
        ensureActive();
        return new Income(id, accountId, categoryId, participantId, description, amount, incomeDate, IncomeStatus.CANCELLED, createdAt, updatedAt);
    }

    public void ensureActive() {
        if (status == IncomeStatus.CANCELLED) {
            throw new BusinessRuleViolationException("INCOME_ALREADY_CANCELLED", "Income is already cancelled.");
        }
        if (status != IncomeStatus.ACTIVE) {
            throw new BusinessRuleViolationException("INCOME_NOT_ACTIVE", "Income is not active.");
        }
    }

    public Long id() { return id; }
    public Long accountId() { return accountId; }
    public Long categoryId() { return categoryId; }
    public Long participantId() { return participantId; }
    public String description() { return description; }
    public Money amount() { return amount; }
    public LocalDate incomeDate() { return incomeDate; }
    public IncomeStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private static Long requireId(Long value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }

    private static Money requirePositiveAmount(Money value) {
        if (value == null || value.amount() == null || value.amount().compareTo(BigDecimal.ZERO) <= 0 || value.currency() != CurrencyCode.COP) {
            throw new BusinessRuleViolationException("INCOME_AMOUNT_INVALID", "Income amount must be greater than zero in COP.");
        }
        return value;
    }

    private static LocalDate requireIncomeDate(LocalDate value) {
        if (value == null) {
            throw new BusinessRuleViolationException("INCOME_DATE_INVALID", "Income date is required.");
        }
        return value;
    }

    private static IncomeStatus requireStatus(IncomeStatus value) {
        if (value == null) {
            throw new BusinessRuleViolationException("INCOME_STATUS_REQUIRED", "Income status is required.");
        }
        return value;
    }
}
