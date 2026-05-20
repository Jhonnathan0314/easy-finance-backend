package com.easyfinance.imports.infrastructure.persistence.jpa;

import com.easyfinance.imports.domain.model.ImportRowError;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "expense_import_rows")
public class ExpenseImportRowJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "batch_id", nullable = false)
    private Long batchId;
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;
    @Column(name = "expense_date")
    private LocalDate expenseDate;
    @Column(name = "description")
    private String description;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "currency")
    private String currency;
    @Column(name = "category_name")
    private String categoryName;
    @Column(name = "category_id")
    private Long categoryId;
    @Column(name = "payment_method_name")
    private String paymentMethodName;
    @Column(name = "payment_method_id")
    private Long paymentMethodId;
    @Column(name = "payment_state")
    private String paymentState;
    @Column(name = "applies_debt_payment", nullable = false)
    private boolean appliesDebtPayment;
    @Column(name = "debt_id")
    private Long debtId;
    @Column(name = "debt_label")
    private String debtLabel;
    @Column(name = "debt_payment_type")
    private String debtPaymentType;
    @Column(name = "debt_payment_notes")
    private String debtPaymentNotes;
    @Column(name = "valid", nullable = false)
    private boolean valid;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "errors_json", columnDefinition = "jsonb")
    private List<ImportRowError> errorsJson;
    @Column(name = "created_expense_id")
    private Long createdExpenseId;
    @Column(name = "created_debt_payment_id")
    private Long createdDebtPaymentId;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    public Long getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public String getPaymentState() { return paymentState; }
    public void setPaymentState(String paymentState) { this.paymentState = paymentState; }
    public boolean isAppliesDebtPayment() { return appliesDebtPayment; }
    public void setAppliesDebtPayment(boolean appliesDebtPayment) { this.appliesDebtPayment = appliesDebtPayment; }
    public Long getDebtId() { return debtId; }
    public void setDebtId(Long debtId) { this.debtId = debtId; }
    public String getDebtLabel() { return debtLabel; }
    public void setDebtLabel(String debtLabel) { this.debtLabel = debtLabel; }
    public String getDebtPaymentType() { return debtPaymentType; }
    public void setDebtPaymentType(String debtPaymentType) { this.debtPaymentType = debtPaymentType; }
    public String getDebtPaymentNotes() { return debtPaymentNotes; }
    public void setDebtPaymentNotes(String debtPaymentNotes) { this.debtPaymentNotes = debtPaymentNotes; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public List<ImportRowError> getErrorsJson() { return errorsJson; }
    public void setErrorsJson(List<ImportRowError> errorsJson) { this.errorsJson = errorsJson; }
    public Long getCreatedExpenseId() { return createdExpenseId; }
    public void setCreatedExpenseId(Long createdExpenseId) { this.createdExpenseId = createdExpenseId; }
    public Long getCreatedDebtPaymentId() { return createdDebtPaymentId; }
    public void setCreatedDebtPaymentId(Long createdDebtPaymentId) { this.createdDebtPaymentId = createdDebtPaymentId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
