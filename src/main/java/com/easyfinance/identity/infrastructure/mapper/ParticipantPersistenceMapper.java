package com.easyfinance.identity.infrastructure.mapper;

import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.infrastructure.persistence.jpa.ParticipantJpaEntity;
import com.easyfinance.identity.infrastructure.persistence.jpa.ParticipantStatusJpa;
import com.easyfinance.identity.infrastructure.persistence.jpa.UserJpaEntity;

public class ParticipantPersistenceMapper {

    public Participant toDomain(ParticipantJpaEntity entity) {
        return Participant.restore(
                entity.getId(),
                entity.getUser().getId(),
                entity.getDisplayName(),
                ParticipantStatus.valueOf(entity.getStatus().name())
        );
    }

    public ParticipantJpaEntity toEntity(Participant participant, UserJpaEntity userEntity) {
        ParticipantJpaEntity entity = new ParticipantJpaEntity();
        entity.setId(participant.id());
        entity.setUser(userEntity);
        entity.setDisplayName(participant.displayName());
        entity.setStatus(ParticipantStatusJpa.valueOf(participant.status().name()));
        return entity;
    }
}

