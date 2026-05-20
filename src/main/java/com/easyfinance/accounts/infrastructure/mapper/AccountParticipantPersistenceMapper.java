package com.easyfinance.accounts.infrastructure.mapper;

import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountJpaEntity;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountParticipantJpaEntity;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountParticipantRoleJpa;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountParticipantStatusJpa;

public class AccountParticipantPersistenceMapper {

    public AccountParticipant toDomain(AccountParticipantJpaEntity entity) {
        return AccountParticipant.restore(
                entity.getId(),
                entity.getAccount().getId(),
                entity.getParticipantId(),
                AccountParticipantRole.valueOf(entity.getRole().name()),
                AccountParticipantStatus.valueOf(entity.getStatus().name()),
                entity.getJoinedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AccountParticipantJpaEntity toEntity(AccountParticipant accountParticipant, AccountJpaEntity accountEntity) {
        AccountParticipantJpaEntity entity = new AccountParticipantJpaEntity();
        copyToEntity(accountParticipant, accountEntity, entity);
        return entity;
    }

    public void copyToEntity(AccountParticipant accountParticipant, AccountJpaEntity accountEntity, AccountParticipantJpaEntity entity) {
        entity.setId(accountParticipant.id());
        entity.setAccount(accountEntity);
        entity.setParticipantId(accountParticipant.participantId());
        entity.setRole(AccountParticipantRoleJpa.valueOf(accountParticipant.role().name()));
        entity.setStatus(AccountParticipantStatusJpa.valueOf(accountParticipant.status().name()));
        entity.setJoinedAt(accountParticipant.joinedAt());
    }
}
