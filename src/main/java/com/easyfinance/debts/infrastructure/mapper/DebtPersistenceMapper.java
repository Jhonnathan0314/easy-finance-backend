package com.easyfinance.debts.infrastructure.mapper;

import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtJpaEntity;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtSourceTypeJpa;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtStateJpa;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

public class DebtPersistenceMapper {

    public Debt toDomain(DebtJpaEntity entity) {
        return Debt.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getParticipantId(),
                entity.getOriginExpenseId(),
                DebtSourceType.valueOf(entity.getSourceType().name()),
                entity.getName(),
                entity.getDescription(),
                new Money(entity.getTotalAmount(), CurrencyCode.valueOf(entity.getTotalCurrency())),
                new Money(entity.getScheduledTotalAmount(), CurrencyCode.valueOf(entity.getTotalCurrency())),
                new Money(entity.getRemainingAmount(), CurrencyCode.valueOf(entity.getRemainingCurrency())),
                entity.getInstallmentCount(),
                entity.getInstallmentAmount() == null ? null : new Money(entity.getInstallmentAmount(), CurrencyCode.valueOf(entity.getInstallmentCurrency())),
                entity.getStartDate(),
                entity.getEndDate(),
                DebtState.valueOf(entity.getState().name()),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DebtJpaEntity toEntity(Debt debt) {
        DebtJpaEntity entity = new DebtJpaEntity();
        copyToEntity(debt, entity);
        return entity;
    }

    public void copyToEntity(Debt debt, DebtJpaEntity entity) {
        entity.setId(debt.id());
        entity.setAccountId(debt.accountId());
        entity.setParticipantId(debt.participantId());
        entity.setOriginExpenseId(debt.originExpenseId());
        entity.setSourceType(DebtSourceTypeJpa.valueOf(debt.sourceType().name()));
        entity.setName(debt.name());
        entity.setDescription(debt.description());
        entity.setTotalAmount(debt.totalAmount().amount());
        entity.setScheduledTotalAmount(debt.scheduledTotalAmount().amount());
        entity.setTotalCurrency(debt.totalAmount().currency().name());
        entity.setRemainingAmount(debt.remainingBalance().amount());
        entity.setRemainingCurrency(debt.remainingBalance().currency().name());
        entity.setInstallmentCount(debt.installmentCount());
        entity.setInstallmentAmount(debt.installmentAmount() == null ? null : debt.installmentAmount().amount());
        entity.setInstallmentCurrency(debt.installmentAmount() == null ? null : debt.installmentAmount().currency().name());
        entity.setStartDate(debt.startDate());
        entity.setEndDate(debt.endDate());
        entity.setState(DebtStateJpa.valueOf(debt.state().name()));
        entity.setNotes(debt.notes());
    }
}
