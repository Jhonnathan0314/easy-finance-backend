package com.easyfinance.expenses.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class Expense {

    private final Long id;
    private final Long accountId;
    private final Long categoryId;
    private final Long paymentMethodId;
    private final Long participantId;
    private final String description;
    private final Money amount;
    private final LocalDate expenseDate;
    private final ExpensePaymentState paymentState;
    private final ExpenseStatus status;
    private final ExpenseType expenseType;
    private final ExpenseSourceType sourceType;
    private final Long sourceDebtPaymentId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Expense(
            Long id,
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            Money amount,
            LocalDate expenseDate,
            ExpensePaymentState paymentState,
            ExpenseStatus status,
            ExpenseType expenseType,
            ExpenseSourceType sourceType,
            Long sourceDebtPaymentId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.accountId = requireId(accountId, "EXPENSE_ACCOUNT_REQUIRED", "Account id is required.");
        this.categoryId = requireId(categoryId, "EXPENSE_CATEGORY_REQUIRED", "Category id is required.");
        this.paymentMethodId = requireId(paymentMethodId, "EXPENSE_PAYMENT_METHOD_REQUIRED", "Payment method id is required.");
        this.participantId = requireId(participantId, "EXPENSE_PARTICIPANT_REQUIRED", "Participant id is required.");
        this.description = ExpenseText.normalizeDescription(description);
        this.amount = requirePositiveAmount(amount);
        this.expenseDate = requireExpenseDate(expenseDate);
        this.paymentState = paymentState == null ? ExpensePaymentState.PAID : paymentState;
        this.status = requireStatus(status);
        this.expenseType = requireType(expenseType);
        this.sourceType = sourceType == null ? ExpenseSourceType.MANUAL : sourceType;
        this.sourceDebtPaymentId = sourceDebtPaymentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validateSourceConsistency();
    }

    public static Expense createSimple(
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            Money amount,
            LocalDate expenseDate,
            ExpensePaymentState paymentState
    ) {
        return new Expense(
                null,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                description,
                amount,
                expenseDate,
                paymentState,
                ExpenseStatus.ACTIVE,
                ExpenseType.SIMPLE,
                ExpenseSourceType.MANUAL,
                null,
                null,
                null
        );
    }

    public static Expense createImported(
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            Money amount,
            LocalDate expenseDate,
            ExpensePaymentState paymentState
    ) {
        return new Expense(
                null,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                description,
                amount,
                expenseDate,
                paymentState,
                ExpenseStatus.ACTIVE,
                ExpenseType.SIMPLE,
                ExpenseSourceType.IMPORT,
                null,
                null,
                null
        );
    }

    public static Expense createDebtPayment(
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            Long debtPaymentId,
            String description,
            Money amount,
            LocalDate expenseDate
    ) {
        return new Expense(
                null,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                description,
                amount,
                expenseDate,
                ExpensePaymentState.PAID,
                ExpenseStatus.ACTIVE,
                ExpenseType.SIMPLE,
                ExpenseSourceType.DEBT_PAYMENT,
                requireId(debtPaymentId, "EXPENSE_SOURCE_DEBT_PAYMENT_REQUIRED", "Source debt payment id is required."),
                null,
                null
        );
    }

    public static Expense createInstallment(
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            Money totalAmount,
            LocalDate expenseDate
    ) {
        return new Expense(
                null,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                description,
                totalAmount,
                expenseDate,
                ExpensePaymentState.PENDING,
                ExpenseStatus.ACTIVE,
                ExpenseType.INSTALLMENT,
                ExpenseSourceType.MANUAL,
                null,
                null,
                null
        );
    }

    public static Expense restore(
            Long id,
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            Money amount,
            LocalDate expenseDate,
            ExpensePaymentState paymentState,
            ExpenseStatus status,
            ExpenseType expenseType,
            ExpenseSourceType sourceType,
            Long sourceDebtPaymentId,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Expense(id, accountId, categoryId, paymentMethodId, participantId, description, amount, expenseDate, paymentState, status, expenseType, sourceType, sourceDebtPaymentId, createdAt, updatedAt);
    }

    public static Expense restore(
            Long id,
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            Money amount,
            LocalDate expenseDate,
            ExpensePaymentState paymentState,
            ExpenseStatus status,
            ExpenseType expenseType,
            Instant createdAt,
            Instant updatedAt
    ) {
        return restore(id, accountId, categoryId, paymentMethodId, participantId, description, amount, expenseDate, paymentState, status, expenseType, ExpenseSourceType.MANUAL, null, createdAt, updatedAt);
    }

    public Expense update(Long categoryId, Long paymentMethodId, String description, Money amount, LocalDate expenseDate, ExpensePaymentState paymentState) {
        ensureActive();
        return new Expense(id, accountId, categoryId, paymentMethodId, participantId, description, amount, expenseDate, paymentState, status, expenseType, sourceType, sourceDebtPaymentId, createdAt, updatedAt);
    }

    public Expense cancel() {
        ensureActive();
        return new Expense(id, accountId, categoryId, paymentMethodId, participantId, description, amount, expenseDate, paymentState, ExpenseStatus.CANCELLED, expenseType, sourceType, sourceDebtPaymentId, createdAt, updatedAt);
    }

    public void ensureActive() {
        if (status == ExpenseStatus.CANCELLED) {
            throw new BusinessRuleViolationException("EXPENSE_ALREADY_CANCELLED", "Expense is already cancelled.");
        }
        if (status != ExpenseStatus.ACTIVE) {
            throw new BusinessRuleViolationException("EXPENSE_NOT_ACTIVE", "Expense is not active.");
        }
    }

    public Long id() {
        return id;
    }

    public Long accountId() {
        return accountId;
    }

    public Long categoryId() {
        return categoryId;
    }

    public Long paymentMethodId() {
        return paymentMethodId;
    }

    public Long participantId() {
        return participantId;
    }

    public String description() {
        return description;
    }

    public Money amount() {
        return amount;
    }

    public LocalDate expenseDate() {
        return expenseDate;
    }

    public ExpensePaymentState paymentState() {
        return paymentState;
    }

    public ExpenseStatus status() {
        return status;
    }

    public ExpenseType expenseType() {
        return expenseType;
    }

    public ExpenseSourceType sourceType() {
        return sourceType;
    }

    public Long sourceDebtPaymentId() {
        return sourceDebtPaymentId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Long requireId(Long value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }

    private static Money requirePositiveAmount(Money value) {
        if (value == null || value.amount() == null || value.amount().compareTo(BigDecimal.ZERO) <= 0 || value.currency() != CurrencyCode.COP) {
            throw new BusinessRuleViolationException("EXPENSE_AMOUNT_INVALID", "Expense amount must be greater than zero in COP.");
        }
        return value;
    }

    private static LocalDate requireExpenseDate(LocalDate value) {
        if (value == null) {
            throw new BusinessRuleViolationException("EXPENSE_DATE_INVALID", "Expense date is required.");
        }
        return value;
    }

    private static ExpenseStatus requireStatus(ExpenseStatus value) {
        if (value == null) {
            throw new BusinessRuleViolationException("EXPENSE_STATUS_REQUIRED", "Expense status is required.");
        }
        return value;
    }

    private static ExpenseType requireType(ExpenseType value) {
        if (value != ExpenseType.SIMPLE && value != ExpenseType.INSTALLMENT) {
            throw new BusinessRuleViolationException("EXPENSE_TYPE_NOT_SUPPORTED", "Expense type is not supported.");
        }
        return value;
    }

    private void validateSourceConsistency() {
        if (sourceType == ExpenseSourceType.DEBT_PAYMENT && sourceDebtPaymentId == null) {
            throw new BusinessRuleViolationException("EXPENSE_SOURCE_DEBT_PAYMENT_REQUIRED", "Source debt payment id is required.");
        }
        if (sourceType != ExpenseSourceType.DEBT_PAYMENT && sourceDebtPaymentId != null) {
            throw new BusinessRuleViolationException("EXPENSE_SOURCE_INVALID", "Only debt payment expenses can reference a debt payment.");
        }
    }
}
