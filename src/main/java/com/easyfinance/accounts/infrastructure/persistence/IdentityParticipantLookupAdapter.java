package com.easyfinance.accounts.infrastructure.persistence;

import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.identity.infrastructure.persistence.jpa.ParticipantJpaEntity;
import com.easyfinance.identity.infrastructure.persistence.jpa.ParticipantStatusJpa;
import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataParticipantRepository;
import com.easyfinance.identity.infrastructure.persistence.jpa.UserStatusJpa;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class IdentityParticipantLookupAdapter implements ParticipantLookupPort {

    private final SpringDataParticipantRepository participantRepository;

    public IdentityParticipantLookupAdapter(SpringDataParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Override
    public Optional<ParticipantInfo> findByParticipantId(Long participantId) {
        return participantRepository.findById(participantId).map(this::toInfo);
    }

    @Override
    public Optional<ParticipantInfo> findActiveByEmail(String email) {
        return participantRepository.findByUserEmail(email)
                .map(this::toInfo)
                .filter(ParticipantInfo::active);
    }

    @Override
    public Map<Long, ParticipantInfo> findByParticipantIds(Collection<Long> participantIds) {
        return participantRepository.findAllById(participantIds)
                .stream()
                .map(this::toInfo)
                .collect(Collectors.toMap(ParticipantInfo::participantId, Function.identity()));
    }

    private ParticipantInfo toInfo(ParticipantJpaEntity entity) {
        boolean active = entity.getStatus() == ParticipantStatusJpa.ACTIVE
                && entity.getUser().getStatus() == UserStatusJpa.ACTIVE;
        return new ParticipantInfo(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getDisplayName(),
                active
        );
    }
}
