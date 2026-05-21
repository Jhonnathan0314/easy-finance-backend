package com.easyfinance.expenses.infrastructure.persistence.jpa;

import com.easyfinance.shared.infrastructure.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class ExpenseJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_state", nullable = false, length = 30)
    private ExpensePaymentStateJpa paymentState;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExpenseStatusJpa status;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 30)
    private ExpenseTypeJpa expenseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ExpenseSourceTypeJpa sourceType;

    @Column(name = "source_debt_payment_id")
    private Long sourceDebtPaymentId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Long paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public ExpensePaymentStateJpa getPaymentState() {
        return paymentState;
    }

    public void setPaymentState(ExpensePaymentStateJpa paymentState) {
        this.paymentState = paymentState;
    }

    public ExpenseStatusJpa getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatusJpa status) {
        this.status = status;
    }

    public ExpenseTypeJpa getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(ExpenseTypeJpa expenseType) {
        this.expenseType = expenseType;
    }

    public ExpenseSourceTypeJpa getSourceType() {
        return sourceType;
    }

    public void setSourceType(ExpenseSourceTypeJpa sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceDebtPaymentId() {
        return sourceDebtPaymentId;
    }

    public void setSourceDebtPaymentId(Long sourceDebtPaymentId) {
        this.sourceDebtPaymentId = sourceDebtPaymentId;
    }
}
