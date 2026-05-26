package com.easyfinance.budgets.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;

public final class SubBudget {

    private final Long id;
    private final Long accountId;
    private final Long budgetId;
    private final Long categoryId;
    private final Long debtId;
    private final String name;
    private final Money plannedAmount;
    private final Money spentAmount;
    private final SubBudgetStatus status;
    private final SubBudgetSourceType sourceType;
    private final Instant createdAt;
    private final Instant updatedAt;

    private SubBudget(Long id, Long accountId, Long budgetId, Long categoryId, Long debtId, String name, Money plannedAmount, Money spentAmount, SubBudgetStatus status, SubBudgetSourceType sourceType, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountId = Budget.requireId(accountId, "SUB_BUDGET_ACCOUNT_REQUIRED", "Sub-budget account is required.");
        this.budgetId = Budget.requireId(budgetId, "SUB_BUDGET_BUDGET_REQUIRED", "Budget id is required.");
        this.categoryId = categoryId;
        this.sourceType = sourceType == null ? SubBudgetSourceType.MANUAL : sourceType;
        this.debtId = validateDebtId(debtId, this.sourceType);
        this.name = requireName(name);
        this.plannedAmount = requireNonNegative(plannedAmount);
        this.spentAmount = requireNonNegative(spentAmount);
        this.status = status == null ? SubBudgetStatus.ACTIVE : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SubBudget createManual(Long accountId, Long budgetId, Long categoryId, String name, Money plannedAmount) {
        return new SubBudget(null, accountId, budgetId, categoryId, null, name, plannedAmount, Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, null, null);
    }

    public static SubBudget createDebtDerived(Long accountId, Long budgetId, Long categoryId, Long debtId, String name, Money plannedAmount) {
        return new SubBudget(null, accountId, budgetId, categoryId, debtId, name, plannedAmount, Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.DEBT_DERIVED, null, null);
    }

    public static SubBudget restore(Long id, Long accountId, Long budgetId, Long categoryId, Long debtId, String name, Money plannedAmount, Money spentAmount, SubBudgetStatus status, SubBudgetSourceType sourceType, Instant createdAt, Instant updatedAt) {
        return new SubBudget(id, accountId, budgetId, categoryId, debtId, name, plannedAmount, spentAmount, status, sourceType, createdAt, updatedAt);
    }

    public SubBudget updateManual(Long categoryId, String name, Money plannedAmount) {
        ensureManualEditable();
        return new SubBudget(id, accountId, budgetId, categoryId, debtId, name, plannedAmount, spentAmount, status, sourceType, createdAt, updatedAt);
    }

    public SubBudget deactivateManual() {
        ensureManualEditable();
        return new SubBudget(id, accountId, budgetId, categoryId, debtId, name, plannedAmount, spentAmount, SubBudgetStatus.INACTIVE, sourceType, createdAt, updatedAt);
    }

    public SubBudget deactivateDebtDerived() {
        if (sourceType != SubBudgetSourceType.DEBT_DERIVED) {
            throw new BusinessRuleViolationException("SUB_BUDGET_SOURCE_NOT_EDITABLE", "Only debt-derived sub-budgets can be deactivated from this operation.");
        }
        return new SubBudget(id, accountId, budgetId, categoryId, debtId, name, plannedAmount, spentAmount, SubBudgetStatus.INACTIVE, sourceType, createdAt, updatedAt);
    }

    public void ensureManualEditable() {
        if (sourceType != SubBudgetSourceType.MANUAL) {
            throw new BusinessRuleViolationException("SUB_BUDGET_SOURCE_NOT_EDITABLE", "Only manual sub-budgets can be edited from this endpoint.");
        }
    }

    public Long id() { return id; }
    public Long accountId() { return accountId; }
    public Long budgetId() { return budgetId; }
    public Long categoryId() { return categoryId; }
    public Long debtId() { return debtId; }
    public String name() { return name; }
    public Money plannedAmount() { return plannedAmount; }
    public Money spentAmount() { return spentAmount; }
    public SubBudgetStatus status() { return status; }
    public SubBudgetSourceType sourceType() { return sourceType; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("SUB_BUDGET_NAME_REQUIRED", "Sub-budget name is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 150) {
            throw new BusinessRuleViolationException("SUB_BUDGET_NAME_INVALID", "Sub-budget name is too long.");
        }
        return trimmed;
    }

    private static Money requireNonNegative(Money value) {
        if (value == null || value.amount() == null || value.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("SUB_BUDGET_AMOUNT_INVALID", "Sub-budget amount cannot be negative.");
        }
        return value;
    }

    private static Long validateDebtId(Long debtId, SubBudgetSourceType sourceType) {
        if (sourceType == SubBudgetSourceType.MANUAL && debtId != null) {
            throw new BusinessRuleViolationException("SUB_BUDGET_SOURCE_INVALID", "Manual sub-budgets cannot be linked to a debt.");
        }
        if (sourceType == SubBudgetSourceType.DEBT_DERIVED) {
            return Budget.requireId(debtId, "SUB_BUDGET_DEBT_REQUIRED", "Debt-derived sub-budget requires a debt id.");
        }
        return null;
    }
}
