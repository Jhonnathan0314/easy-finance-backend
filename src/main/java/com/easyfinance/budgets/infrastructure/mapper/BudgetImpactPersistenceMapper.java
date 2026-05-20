package com.easyfinance.budgets.infrastructure.mapper;

import com.easyfinance.budgets.domain.model.BudgetImpact;
import com.easyfinance.budgets.domain.model.BudgetImpactSourceType;
import com.easyfinance.budgets.domain.model.BudgetImpactStatus;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetImpactJpaEntity;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetImpactSourceTypeJpa;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetImpactStatusJpa;
import com.easyfinance.shared.domain.Money;

public class BudgetImpactPersistenceMapper {

    public BudgetImpact toDomain(BudgetImpactJpaEntity entity) {
        return BudgetImpact.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getBudgetId(),
                entity.getSubBudgetId(),
                entity.getDebtId(),
                entity.getExpenseId(),
                entity.getPeriodYear(),
                entity.getPeriodMonth(),
                Money.cop(entity.getExpectedAmount()),
                Money.cop(entity.getPaidAmount()),
                BudgetImpactStatus.valueOf(entity.getStatus().name()),
                BudgetImpactSourceType.valueOf(entity.getSourceType().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BudgetImpactJpaEntity toEntity(BudgetImpact impact) {
        BudgetImpactJpaEntity entity = new BudgetImpactJpaEntity();
        copyToEntity(impact, entity);
        return entity;
    }

    public void copyToEntity(BudgetImpact impact, BudgetImpactJpaEntity entity) {
        entity.setId(impact.id());
        entity.setAccountId(impact.accountId());
        entity.setBudgetId(impact.budgetId());
        entity.setSubBudgetId(impact.subBudgetId());
        entity.setDebtId(impact.debtId());
        entity.setExpenseId(impact.expenseId());
        entity.setPeriodYear(impact.periodYear());
        entity.setPeriodMonth(impact.periodMonth());
        entity.setExpectedAmount(impact.expectedAmount().amount());
        entity.setExpectedCurrency(impact.expectedAmount().currency().name());
        entity.setPaidAmount(impact.paidAmount().amount());
        entity.setPaidCurrency(impact.paidAmount().currency().name());
        entity.setStatus(BudgetImpactStatusJpa.valueOf(impact.status().name()));
        entity.setSourceType(BudgetImpactSourceTypeJpa.valueOf(impact.sourceType().name()));
    }
}
