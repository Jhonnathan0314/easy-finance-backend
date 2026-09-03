package com.easyfinance.debts.infrastructure.mapper;

import com.easyfinance.debts.domain.model.DebtPayment;
import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtPaymentJpaEntity;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtPaymentStatusJpa;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtPaymentTypeJpa;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

public class DebtPaymentPersistenceMapper {

    public DebtPayment toDomain(DebtPaymentJpaEntity entity) {
        CurrencyCode currency = CurrencyCode.valueOf(entity.getCurrency());
        return DebtPayment.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getDebtId(),
                entity.getParticipantId(),
                DebtPaymentType.valueOf(entity.getPaymentType().name()),
                new Money(entity.getCapitalAmount(), currency),
                new Money(entity.getInterestAmount(), currency),
                entity.getPaymentDate(),
                entity.getNotes(),
                DebtPaymentStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DebtPaymentJpaEntity toEntity(DebtPayment payment) {
        DebtPaymentJpaEntity entity = new DebtPaymentJpaEntity();
        copyToEntity(payment, entity);
        return entity;
    }

    public void copyToEntity(DebtPayment payment, DebtPaymentJpaEntity entity) {
        entity.setId(payment.id());
        entity.setAccountId(payment.accountId());
        entity.setDebtId(payment.debtId());
        entity.setParticipantId(payment.participantId());
        entity.setPaymentType(DebtPaymentTypeJpa.valueOf(payment.paymentType().name()));
        entity.setAmount(payment.amount().amount());
        entity.setCapitalAmount(payment.capitalAmount().amount());
        entity.setInterestAmount(payment.interestAmount().amount());
        entity.setCurrency(payment.amount().currency().name());
        entity.setPaymentDate(payment.paymentDate());
        entity.setNotes(payment.notes());
        entity.setStatus(DebtPaymentStatusJpa.valueOf(payment.status().name()));
    }
}
