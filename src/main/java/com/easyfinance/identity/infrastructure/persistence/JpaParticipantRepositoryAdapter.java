package com.easyfinance.identity.infrastructure.persistence;

import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.infrastructure.mapper.ParticipantPersistenceMapper;
import com.easyfinance.identity.infrastructure.persistence.jpa.ParticipantJpaEntity;
import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataParticipantRepository;
import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.easyfinance.identity.infrastructure.persistence.jpa.UserJpaEntity;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaParticipantRepositoryAdapter implements ParticipantRepositoryPort {

    private final SpringDataParticipantRepository participantRepository;
    private final SpringDataUserRepository userRepository;
    private final ParticipantPersistenceMapper mapper = new ParticipantPersistenceMapper();

    public JpaParticipantRepositoryAdapter(
            SpringDataParticipantRepository participantRepository,
            SpringDataUserRepository userRepository
    ) {
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Participant save(Participant participant) {
        UserJpaEntity userEntity = userRepository.findById(participant.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User was not found."));
        ParticipantJpaEntity saved = participantRepository.save(mapper.toEntity(participant, userEntity));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Participant> findByUserId(Long userId) {
        return participantRepository.findByUserId(userId).map(mapper::toDomain);
    }
}

