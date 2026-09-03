package com.easyfinance.debts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class DebtPayment {

    private final Long id;
    private final Long accountId;
    private final Long debtId;
    private final Long participantId;
    private final DebtPaymentType paymentType;
    private final Money capitalAmount;
    private final Money interestAmount;
    private final LocalDate paymentDate;
    private final String notes;
    private final DebtPaymentStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private DebtPayment(
            Long id,
            Long accountId,
            Long debtId,
            Long participantId,
            DebtPaymentType paymentType,
            Money capitalAmount,
            Money interestAmount,
            LocalDate paymentDate,
            String notes,
            DebtPaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.accountId = requireId(accountId, "DEBT_PAYMENT_ACCOUNT_REQUIRED", "Account id is required.");
        this.debtId = requireId(debtId, "DEBT_PAYMENT_DEBT_REQUIRED", "Debt id is required.");
        this.participantId = requireId(participantId, "DEBT_PAYMENT_PARTICIPANT_REQUIRED", "Participant id is required.");
        this.paymentType = requirePaymentType(paymentType);
        this.capitalAmount = requirePositiveAmount(capitalAmount);
        this.interestAmount = requireNonNegativeAmount(interestAmount);
        requireNoInterestOnCapitalPayment(this.paymentType, this.interestAmount);
        this.paymentDate = requirePaymentDate(paymentDate);
        this.notes = DebtText.normalizeNotes(notes);
        this.status = requireStatus(status);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DebtPayment create(
            Long accountId,
            Long debtId,
            Long participantId,
            DebtPaymentType paymentType,
            Money capitalAmount,
            Money interestAmount,
            LocalDate paymentDate,
            String notes
    ) {
        return new DebtPayment(null, accountId, debtId, participantId, paymentType, capitalAmount, interestAmount, paymentDate, notes, DebtPaymentStatus.ACTIVE, null, null);
    }

    public static DebtPayment restore(
            Long id,
            Long accountId,
            Long debtId,
            Long participantId,
            DebtPaymentType paymentType,
            Money capitalAmount,
            Money interestAmount,
            LocalDate paymentDate,
            String notes,
            DebtPaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DebtPayment(id, accountId, debtId, participantId, paymentType, capitalAmount, interestAmount, paymentDate, notes, status, createdAt, updatedAt);
    }

    public Long id() { return id; }
    public Long accountId() { return accountId; }
    public Long debtId() { return debtId; }
    public Long participantId() { return participantId; }
    public DebtPaymentType paymentType() { return paymentType; }
    public Money capitalAmount() { return capitalAmount; }
    public Money interestAmount() { return interestAmount; }
    public Money amount() { return capitalAmount.plus(interestAmount); }
    public LocalDate paymentDate() { return paymentDate; }
    public String notes() { return notes; }
    public DebtPaymentStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private static Long requireId(Long value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }

    private static DebtPaymentType requirePaymentType(DebtPaymentType value) {
        if (value == null) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_TYPE_INVALID", "Debt payment type is required.");
        }
        return value;
    }

    private static Money requirePositiveAmount(Money value) {
        if (value == null || value.amount() == null || value.amount().compareTo(BigDecimal.ZERO) <= 0 || value.currency() != CurrencyCode.COP) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_AMOUNT_INVALID", "Debt payment amount must be greater than zero in COP.");
        }
        return value;
    }

    private static Money requireNonNegativeAmount(Money value) {
        if (value == null || value.amount() == null || value.amount().compareTo(BigDecimal.ZERO) < 0 || value.currency() != CurrencyCode.COP) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_INTEREST_AMOUNT_INVALID", "Debt payment interest amount cannot be negative and must be in COP.");
        }
        return value;
    }

    private static void requireNoInterestOnCapitalPayment(DebtPaymentType paymentType, Money interestAmount) {
        if (paymentType == DebtPaymentType.CAPITAL_PAYMENT && interestAmount.amount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_CAPITAL_PAYMENT_INTEREST_NOT_ALLOWED", "A capital payment cannot carry an interest amount.");
        }
    }

    private static LocalDate requirePaymentDate(LocalDate value) {
        if (value == null) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_DATE_INVALID", "Debt payment date is required.");
        }
        return value;
    }

    private static DebtPaymentStatus requireStatus(DebtPaymentStatus value) {
        if (value == null) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_STATUS_INVALID", "Debt payment status is required.");
        }
        return value;
    }
}
