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
@Table(name = "sub_budgets")
public class SubBudgetJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "participant_id")
    private Long participantId;

    @Column(name = "debt_id")
    private Long debtId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "planned_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal plannedAmount;

    @Column(name = "planned_currency", nullable = false, length = 3)
    private String plannedCurrency;

    @Column(name = "spent_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal spentAmount;

    @Column(name = "spent_currency", nullable = false, length = 3)
    private String spentCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubBudgetStatusJpa status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SubBudgetSourceTypeJpa sourceType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }
    public Long getDebtId() { return debtId; }
    public void setDebtId(Long debtId) { this.debtId = debtId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }
    public String getPlannedCurrency() { return plannedCurrency; }
    public void setPlannedCurrency(String plannedCurrency) { this.plannedCurrency = plannedCurrency; }
    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
    public String getSpentCurrency() { return spentCurrency; }
    public void setSpentCurrency(String spentCurrency) { this.spentCurrency = spentCurrency; }
    public SubBudgetStatusJpa getStatus() { return status; }
    public void setStatus(SubBudgetStatusJpa status) { this.status = status; }
    public SubBudgetSourceTypeJpa getSourceType() { return sourceType; }
    public void setSourceType(SubBudgetSourceTypeJpa sourceType) { this.sourceType = sourceType; }
}
