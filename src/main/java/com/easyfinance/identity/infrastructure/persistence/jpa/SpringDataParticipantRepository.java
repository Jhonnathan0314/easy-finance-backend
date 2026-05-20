package com.easyfinance.identity.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataParticipantRepository extends JpaRepository<ParticipantJpaEntity, Long> {

    Optional<ParticipantJpaEntity> findByUserId(Long userId);

    Optional<ParticipantJpaEntity> findByUserEmail(String email);
}
