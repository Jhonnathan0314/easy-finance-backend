package com.easyfinance.budgets.infrastructure.mapper;

import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetJpaEntity;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetStatusJpa;

public class BudgetPersistenceMapper {

    public Budget toDomain(BudgetJpaEntity entity) {
        return Budget.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getYear(),
                entity.getMonth(),
                entity.getName(),
                BudgetStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BudgetJpaEntity toEntity(Budget budget) {
        BudgetJpaEntity entity = new BudgetJpaEntity();
        copyToEntity(budget, entity);
        return entity;
    }

    public void copyToEntity(Budget budget, BudgetJpaEntity entity) {
        entity.setId(budget.id());
        entity.setAccountId(budget.accountId());
        entity.setYear(budget.year());
        entity.setMonth(budget.month());
        entity.setName(budget.name());
        entity.setStatus(BudgetStatusJpa.valueOf(budget.status().name()));
    }
}
