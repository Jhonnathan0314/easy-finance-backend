package com.easyfinance.budgets.infrastructure.persistence.jpa;

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

@Entity
@Table(name = "budget_impacts")
public class BudgetImpactJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "sub_budget_id", nullable = false)
    private Long subBudgetId;

    @Column(name = "debt_id", nullable = false)
    private Long debtId;

    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "expected_currency", nullable = false, length = 3)
    private String expectedCurrency;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_currency", nullable = false, length = 3)
    private String paidCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BudgetImpactStatusJpa status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private BudgetImpactSourceTypeJpa sourceType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    public Long getSubBudgetId() { return subBudgetId; }
    public void setSubBudgetId(Long subBudgetId) { this.subBudgetId = subBudgetId; }
    public Long getDebtId() { return debtId; }
    public void setDebtId(Long debtId) { this.debtId = debtId; }
    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public String getExpectedCurrency() { return expectedCurrency; }
    public void setExpectedCurrency(String expectedCurrency) { this.expectedCurrency = expectedCurrency; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getPaidCurrency() { return paidCurrency; }
    public void setPaidCurrency(String paidCurrency) { this.paidCurrency = paidCurrency; }
    public BudgetImpactStatusJpa getStatus() { return status; }
    public void setStatus(BudgetImpactStatusJpa status) { this.status = status; }
    public BudgetImpactSourceTypeJpa getSourceType() { return sourceType; }
    public void setSourceType(BudgetImpactSourceTypeJpa sourceType) { this.sourceType = sourceType; }
}
