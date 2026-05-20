package com.easyfinance.budgets.infrastructure.mapper;

import com.easyfinance.budgets.domain.model.SubBudget;
import com.easyfinance.budgets.domain.model.SubBudgetSourceType;
import com.easyfinance.budgets.domain.model.SubBudgetStatus;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SubBudgetJpaEntity;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SubBudgetSourceTypeJpa;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SubBudgetStatusJpa;
import com.easyfinance.shared.domain.Money;

public class SubBudgetPersistenceMapper {

    public SubBudget toDomain(SubBudgetJpaEntity entity) {
        return SubBudget.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getBudgetId(),
                entity.getCategoryId(),
                entity.getDebtId(),
                entity.getName(),
                Money.cop(entity.getPlannedAmount()),
                Money.cop(entity.getSpentAmount()),
                SubBudgetStatus.valueOf(entity.getStatus().name()),
                SubBudgetSourceType.valueOf(entity.getSourceType().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SubBudgetJpaEntity toEntity(SubBudget subBudget) {
        SubBudgetJpaEntity entity = new SubBudgetJpaEntity();
        copyToEntity(subBudget, entity);
        return entity;
    }

    public void copyToEntity(SubBudget subBudget, SubBudgetJpaEntity entity) {
        entity.setId(subBudget.id());
        entity.setAccountId(subBudget.accountId());
        entity.setBudgetId(subBudget.budgetId());
        entity.setCategoryId(subBudget.categoryId());
        entity.setDebtId(subBudget.debtId());
        entity.setName(subBudget.name());
        entity.setPlannedAmount(subBudget.plannedAmount().amount());
        entity.setPlannedCurrency(subBudget.plannedAmount().currency().name());
        entity.setSpentAmount(subBudget.spentAmount().amount());
        entity.setSpentCurrency(subBudget.spentAmount().currency().name());
        entity.setStatus(SubBudgetStatusJpa.valueOf(subBudget.status().name()));
        entity.setSourceType(SubBudgetSourceTypeJpa.valueOf(subBudget.sourceType().name()));
    }
}
