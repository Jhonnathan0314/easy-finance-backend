package com.easyfinance.debts.infrastructure.persistence.jpa;

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
@Table(name = "debts")
public class DebtJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "origin_expense_id")
    private Long originExpenseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private DebtSourceTypeJpa sourceType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_currency", nullable = false, length = 3)
    private String totalCurrency;

    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "remaining_currency", nullable = false, length = 3)
    private String remainingCurrency;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Column(name = "installment_amount", precision = 19, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "installment_currency", length = 3)
    private String installmentCurrency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private DebtStateJpa state;

    @Column(name = "notes", length = 1000)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }
    public Long getOriginExpenseId() { return originExpenseId; }
    public void setOriginExpenseId(Long originExpenseId) { this.originExpenseId = originExpenseId; }
    public DebtSourceTypeJpa getSourceType() { return sourceType; }
    public void setSourceType(DebtSourceTypeJpa sourceType) { this.sourceType = sourceType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getTotalCurrency() { return totalCurrency; }
    public void setTotalCurrency(String totalCurrency) { this.totalCurrency = totalCurrency; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public String getRemainingCurrency() { return remainingCurrency; }
    public void setRemainingCurrency(String remainingCurrency) { this.remainingCurrency = remainingCurrency; }
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }
    public String getInstallmentCurrency() { return installmentCurrency; }
    public void setInstallmentCurrency(String installmentCurrency) { this.installmentCurrency = installmentCurrency; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public DebtStateJpa getState() { return state; }
    public void setState(DebtStateJpa state) { this.state = state; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
