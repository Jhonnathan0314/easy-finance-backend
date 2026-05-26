package com.easyfinance.debts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class Debt {

    private final Long id;
    private final Long accountId;
    private final Long participantId;
    private final Long originExpenseId;
    private final DebtSourceType sourceType;
    private final String name;
    private final String description;
    private final Money totalAmount;
    private final Money scheduledTotalAmount;
    private final Money remainingBalance;
    private final Integer installmentCount;
    private final Money installmentAmount;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final DebtState state;
    private final String notes;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Debt(
            Long id,
            Long accountId,
            Long participantId,
            Long originExpenseId,
            DebtSourceType sourceType,
            String name,
            String description,
            Money totalAmount,
            Money scheduledTotalAmount,
            Money remainingBalance,
            Integer installmentCount,
            Money installmentAmount,
            LocalDate startDate,
            LocalDate endDate,
            DebtState state,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.accountId = requireId(accountId, "DEBT_ACCOUNT_REQUIRED", "Account id is required.");
        this.participantId = requireId(participantId, "DEBT_PARTICIPANT_REQUIRED", "Participant id is required.");
        this.originExpenseId = originExpenseId;
        this.sourceType = requireSource(sourceType);
        this.name = DebtText.normalizeName(name);
        this.description = DebtText.normalizeDescription(description);
        this.totalAmount = requirePositiveAmount(totalAmount, "DEBT_AMOUNT_INVALID", "Debt total amount must be greater than zero in COP.");
        this.scheduledTotalAmount = requireScheduledTotalAmount(scheduledTotalAmount, this.totalAmount);
        this.remainingBalance = requireRemainingBalance(remainingBalance, this.totalAmount);
        this.installmentCount = validateInstallmentCount(installmentCount, sourceType);
        this.installmentAmount = validateInstallmentAmount(installmentAmount, sourceType);
        this.startDate = requireStartDate(startDate);
        this.endDate = endDate;
        this.state = requireState(state);
        this.notes = DebtText.normalizeNotes(notes);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validateSourceConsistency();
    }

    public static Debt createManual(
            Long accountId,
            Long participantId,
            String name,
            String description,
            Money totalAmount,
            Integer installmentCount,
            Money installmentAmount,
            LocalDate startDate,
            LocalDate dueDate,
            String notes
    ) {
        LocalDate endDate = installmentCount == null ? dueDate : calculateEndDate(startDate, installmentCount);
        Money scheduledTotal = scheduledTotalAmount(totalAmount, installmentCount, installmentAmount);
        return new Debt(null, accountId, participantId, null, DebtSourceType.MANUAL, name, description, totalAmount, scheduledTotal, totalAmount, installmentCount, installmentAmount, startDate, endDate, DebtState.ACTIVE, notes, null, null);
    }

    public static Debt createFromInstallmentExpense(
            Long accountId,
            Long participantId,
            Long originExpenseId,
            String name,
            String description,
            Money principalAmount,
            Integer installmentCount,
            Money installmentAmount,
            LocalDate firstInstallmentDate,
            String notes
    ) {
        LocalDate endDate = calculateEndDate(firstInstallmentDate, installmentCount);
        Money scheduledTotal = financedTotal(installmentAmount, installmentCount, principalAmount);
        return new Debt(null, accountId, participantId, originExpenseId, DebtSourceType.INSTALLMENT_EXPENSE, name, description, principalAmount, scheduledTotal, principalAmount, installmentCount, installmentAmount, firstInstallmentDate, endDate, DebtState.ACTIVE, notes, null, null);
    }

    public static Debt restore(
            Long id,
            Long accountId,
            Long participantId,
            Long originExpenseId,
            DebtSourceType sourceType,
            String name,
            String description,
            Money totalAmount,
            Money scheduledTotalAmount,
            Money remainingBalance,
            Integer installmentCount,
            Money installmentAmount,
            LocalDate startDate,
            LocalDate endDate,
            DebtState state,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Debt(id, accountId, participantId, originExpenseId, sourceType, name, description, totalAmount, scheduledTotalAmount, remainingBalance, installmentCount, installmentAmount, startDate, endDate, state, notes, createdAt, updatedAt);
    }

    public Debt cancel() {
        ensureActive();
        return new Debt(id, accountId, participantId, originExpenseId, sourceType, name, description, totalAmount, scheduledTotalAmount, remainingBalance, installmentCount, installmentAmount, startDate, endDate, DebtState.CANCELLED, notes, createdAt, updatedAt);
    }

    public Debt applyPayment(Money paymentAmount) {
        ensurePayable();
        Money resolvedPaymentAmount = requirePositiveAmount(paymentAmount, "DEBT_PAYMENT_AMOUNT_INVALID", "Debt payment amount must be greater than zero in COP.");
        if (resolvedPaymentAmount.amount().compareTo(remainingBalance.amount()) > 0) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE", "Debt payment exceeds remaining balance.");
        }
        BigDecimal newRemainingAmount = remainingBalance.amount().subtract(resolvedPaymentAmount.amount());
        Money newRemainingBalance = Money.cop(newRemainingAmount);
        DebtState newState = newRemainingAmount.signum() == 0 ? DebtState.PAID : DebtState.ACTIVE;
        return new Debt(id, accountId, participantId, originExpenseId, sourceType, name, description, totalAmount, scheduledTotalAmount, newRemainingBalance, installmentCount, installmentAmount, startDate, endDate, newState, notes, createdAt, updatedAt);
    }

    public void ensureActive() {
        if (state == DebtState.CANCELLED) {
            throw new BusinessRuleViolationException("DEBT_ALREADY_CANCELLED", "Debt is already cancelled.");
        }
        if (state != DebtState.ACTIVE) {
            throw new BusinessRuleViolationException("DEBT_NOT_ACTIVE", "Debt is not active.");
        }
    }

    private void ensurePayable() {
        if (state == DebtState.CANCELLED) {
            throw new BusinessRuleViolationException("DEBT_CANCELLED", "Debt is cancelled.");
        }
        if (state == DebtState.PAID) {
            throw new BusinessRuleViolationException("DEBT_ALREADY_PAID", "Debt is already paid.");
        }
        if (state != DebtState.ACTIVE) {
            throw new BusinessRuleViolationException("DEBT_NOT_ACTIVE", "Debt is not active.");
        }
    }

    public static LocalDate calculateEndDate(LocalDate startDate, Integer installmentCount) {
        LocalDate resolvedStartDate = requireStartDate(startDate);
        Integer resolvedInstallmentCount = validateInstallmentCount(installmentCount, DebtSourceType.INSTALLMENT_EXPENSE);
        return resolvedStartDate.plusMonths(resolvedInstallmentCount);
    }

    public Long id() { return id; }
    public Long accountId() { return accountId; }
    public Long participantId() { return participantId; }
    public Long originExpenseId() { return originExpenseId; }
    public DebtSourceType sourceType() { return sourceType; }
    public String name() { return name; }
    public String description() { return description; }
    public Money totalAmount() { return totalAmount; }
    public Money scheduledTotalAmount() { return scheduledTotalAmount; }
    public Money remainingBalance() { return remainingBalance; }
    public Integer installmentCount() { return installmentCount; }
    public Money installmentAmount() { return installmentAmount; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public DebtState state() { return state; }
    public String notes() { return notes; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private void validateSourceConsistency() {
        if (sourceType == DebtSourceType.MANUAL && originExpenseId != null) {
            throw new BusinessRuleViolationException("DEBT_SOURCE_INVALID", "Manual debt cannot have origin expense.");
        }
        if (sourceType == DebtSourceType.INSTALLMENT_EXPENSE && originExpenseId == null) {
            throw new BusinessRuleViolationException("DEBT_SOURCE_INVALID", "Installment expense debt requires origin expense.");
        }
        if (sourceType == DebtSourceType.INSTALLMENT_EXPENSE && (installmentCount == null || installmentAmount == null)) {
            throw new BusinessRuleViolationException("DEBT_SOURCE_INVALID", "Installment expense debt requires installment data.");
        }
    }

    private static Long requireId(Long value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }

    private static DebtSourceType requireSource(DebtSourceType value) {
        if (value == null) {
            throw new BusinessRuleViolationException("DEBT_SOURCE_INVALID", "Debt source is required.");
        }
        return value;
    }

    private static DebtState requireState(DebtState value) {
        if (value == null) {
            throw new BusinessRuleViolationException("DEBT_STATE_REQUIRED", "Debt state is required.");
        }
        return value;
    }

    private static Money requirePositiveAmount(Money value, String code, String message) {
        if (value == null || value.amount() == null || value.amount().compareTo(BigDecimal.ZERO) <= 0 || value.currency() != CurrencyCode.COP) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }

    private static Money requireRemainingBalance(Money remaining, Money total) {
        Money resolved = remaining == null ? total : remaining;
        if (resolved.currency() != total.currency() || resolved.amount().compareTo(BigDecimal.ZERO) < 0 || resolved.amount().compareTo(total.amount()) > 0) {
            throw new BusinessRuleViolationException("DEBT_AMOUNT_INVALID", "Debt remaining balance is invalid.");
        }
        return resolved;
    }

    private static Money requireScheduledTotalAmount(Money scheduled, Money total) {
        Money resolved = scheduled == null ? total : scheduled;
        if (resolved.currency() != total.currency() || resolved.amount().compareTo(total.amount()) < 0) {
            throw new BusinessRuleViolationException("DEBT_SCHEDULED_TOTAL_INVALID", "Debt scheduled total amount cannot be lower than debt total amount.");
        }
        return resolved;
    }

    private static Money scheduledTotalAmount(Money totalAmount, Integer installmentCount, Money installmentAmount) {
        if (installmentCount == null || installmentAmount == null) {
            return totalAmount;
        }
        return financedTotal(installmentAmount, installmentCount, totalAmount);
    }

    private static Money financedTotal(Money installmentAmount, Integer installmentCount, Money principalAmount) {
        BigDecimal financedAmount = installmentAmount.amount().multiply(BigDecimal.valueOf(installmentCount));
        if (financedAmount.compareTo(principalAmount.amount()) < 0) {
            throw new BusinessRuleViolationException("DEBT_SCHEDULED_TOTAL_INVALID", "Debt scheduled total amount cannot be lower than debt total amount.");
        }
        return Money.cop(financedAmount);
    }

    private static Integer validateInstallmentCount(Integer value, DebtSourceType sourceType) {
        if (value == null) {
            if (sourceType == DebtSourceType.INSTALLMENT_EXPENSE) {
                throw new BusinessRuleViolationException("DEBT_INSTALLMENT_COUNT_INVALID", "Debt installment count is required.");
            }
            return null;
        }
        if (value <= 0) {
            throw new BusinessRuleViolationException("DEBT_INSTALLMENT_COUNT_INVALID", "Debt installment count must be greater than zero.");
        }
        return value;
    }

    private static Money validateInstallmentAmount(Money value, DebtSourceType sourceType) {
        if (value == null) {
            if (sourceType == DebtSourceType.INSTALLMENT_EXPENSE) {
                throw new BusinessRuleViolationException("DEBT_INSTALLMENT_AMOUNT_INVALID", "Debt installment amount is required.");
            }
            return null;
        }
        return requirePositiveAmount(value, "DEBT_INSTALLMENT_AMOUNT_INVALID", "Debt installment amount must be greater than zero in COP.");
    }

    private static LocalDate requireStartDate(LocalDate value) {
        if (value == null) {
            throw new BusinessRuleViolationException("DEBT_DATE_INVALID", "Debt start date is required.");
        }
        return value;
    }
}
