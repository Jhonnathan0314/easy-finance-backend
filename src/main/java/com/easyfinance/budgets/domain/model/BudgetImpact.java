package com.easyfinance.budgets.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;

public final class BudgetImpact {

    private final Long id;
    private final Long accountId;
    private final Long budgetId;
    private final Long subBudgetId;
    private final Long debtId;
    private final Long expenseId;
    private final Integer periodYear;
    private final Integer periodMonth;
    private final Money expectedAmount;
    private final Money paidAmount;
    private final BudgetImpactStatus status;
    private final BudgetImpactSourceType sourceType;
    private final Instant createdAt;
    private final Instant updatedAt;

    private BudgetImpact(Long id, Long accountId, Long budgetId, Long subBudgetId, Long debtId, Long expenseId, Integer periodYear, Integer periodMonth, Money expectedAmount, Money paidAmount, BudgetImpactStatus status, BudgetImpactSourceType sourceType, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountId = Budget.requireId(accountId, "BUDGET_IMPACT_ACCOUNT_REQUIRED", "Budget impact account is required.");
        this.budgetId = Budget.requireId(budgetId, "BUDGET_IMPACT_BUDGET_REQUIRED", "Budget id is required.");
        this.subBudgetId = Budget.requireId(subBudgetId, "BUDGET_IMPACT_SUB_BUDGET_REQUIRED", "Sub-budget id is required.");
        this.debtId = Budget.requireId(debtId, "BUDGET_IMPACT_DEBT_REQUIRED", "Debt id is required.");
        this.expenseId = expenseId;
        this.periodYear = Budget.requireYear(periodYear);
        this.periodMonth = Budget.requireMonth(periodMonth);
        this.expectedAmount = requirePositive(expectedAmount);
        this.paidAmount = requirePaidAmount(paidAmount, expectedAmount);
        this.status = resolveStatus(status, this.paidAmount, this.expectedAmount);
        this.sourceType = sourceType == null ? BudgetImpactSourceType.DEBT_INSTALLMENT : sourceType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BudgetImpact createDebtInstallment(Long accountId, Long budgetId, Long subBudgetId, Long debtId, Long expenseId, Integer periodYear, Integer periodMonth, Money expectedAmount) {
        return new BudgetImpact(null, accountId, budgetId, subBudgetId, debtId, expenseId, periodYear, periodMonth, expectedAmount, Money.zeroCop(), BudgetImpactStatus.ACTIVE, BudgetImpactSourceType.DEBT_INSTALLMENT, null, null);
    }

    public static BudgetImpact restore(Long id, Long accountId, Long budgetId, Long subBudgetId, Long debtId, Long expenseId, Integer periodYear, Integer periodMonth, Money expectedAmount, Money paidAmount, BudgetImpactStatus status, BudgetImpactSourceType sourceType, Instant createdAt, Instant updatedAt) {
        return new BudgetImpact(id, accountId, budgetId, subBudgetId, debtId, expenseId, periodYear, periodMonth, expectedAmount, paidAmount, status, sourceType, createdAt, updatedAt);
    }

    public BudgetImpact applyPayment(Money amount) {
        if (amount == null || amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_AMOUNT_INVALID", "Budget impact payment amount must be positive.");
        }
        BigDecimal unpaid = expectedAmount.amount().subtract(paidAmount.amount());
        BigDecimal applied = amount.amount().min(unpaid);
        Money newPaidAmount = Money.cop(paidAmount.amount().add(applied));
        return new BudgetImpact(id, accountId, budgetId, subBudgetId, debtId, expenseId, periodYear, periodMonth, expectedAmount, newPaidAmount, null, sourceType, createdAt, updatedAt);
    }

    public Money unpaidAmount() {
        return Money.cop(expectedAmount.amount().subtract(paidAmount.amount()));
    }

    public Long id() { return id; }
    public Long accountId() { return accountId; }
    public Long budgetId() { return budgetId; }
    public Long subBudgetId() { return subBudgetId; }
    public Long debtId() { return debtId; }
    public Long expenseId() { return expenseId; }
    public Integer periodYear() { return periodYear; }
    public Integer periodMonth() { return periodMonth; }
    public Money expectedAmount() { return expectedAmount; }
    public Money paidAmount() { return paidAmount; }
    public BudgetImpactStatus status() { return status; }
    public BudgetImpactSourceType sourceType() { return sourceType; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private static Money requirePositive(Money value) {
        if (value == null || value.amount() == null || value.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_AMOUNT_INVALID", "Budget impact expected amount must be positive.");
        }
        return value;
    }

    private static Money requirePaidAmount(Money paidAmount, Money expectedAmount) {
        Money resolved = paidAmount == null ? Money.zeroCop() : paidAmount;
        if (resolved.amount().compareTo(BigDecimal.ZERO) < 0 || resolved.amount().compareTo(expectedAmount.amount()) > 0) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_AMOUNT_INVALID", "Budget impact paid amount is invalid.");
        }
        return resolved;
    }

    private static BudgetImpactStatus resolveStatus(BudgetImpactStatus status, Money paidAmount, Money expectedAmount) {
        if (status == BudgetImpactStatus.CANCELLED) {
            return status;
        }
        return paidAmount.amount().compareTo(expectedAmount.amount()) == 0 ? BudgetImpactStatus.PAID : BudgetImpactStatus.ACTIVE;
    }
}
