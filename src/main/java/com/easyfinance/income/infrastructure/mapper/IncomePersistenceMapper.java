package com.easyfinance.income.infrastructure.mapper;

import com.easyfinance.income.domain.model.Income;
import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.income.infrastructure.persistence.jpa.IncomeJpaEntity;
import com.easyfinance.income.infrastructure.persistence.jpa.IncomeStatusJpa;
import com.easyfinance.shared.domain.Money;

public class IncomePersistenceMapper {

    public Income toDomain(IncomeJpaEntity entity) {
        return Income.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getCategoryId(),
                entity.getParticipantId(),
                entity.getDescription(),
                Money.cop(entity.getAmount()),
                entity.getIncomeDate(),
                IncomeStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public IncomeJpaEntity toEntity(Income income) {
        IncomeJpaEntity entity = new IncomeJpaEntity();
        copyToEntity(income, entity);
        return entity;
    }

    public void copyToEntity(Income income, IncomeJpaEntity entity) {
        entity.setId(income.id());
        entity.setAccountId(income.accountId());
        entity.setCategoryId(income.categoryId());
        entity.setParticipantId(income.participantId());
        entity.setDescription(income.description());
        entity.setAmount(income.amount().amount());
        entity.setCurrency(income.amount().currency().name());
        entity.setIncomeDate(income.incomeDate());
        entity.setStatus(IncomeStatusJpa.valueOf(income.status().name()));
    }
}
