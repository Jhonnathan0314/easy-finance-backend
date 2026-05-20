package com.easyfinance.identity.application.port.out;

import com.easyfinance.identity.domain.model.Participant;

import java.util.Optional;

public interface ParticipantRepositoryPort {

    Participant save(Participant participant);

    Optional<Participant> findByUserId(Long userId);
}

