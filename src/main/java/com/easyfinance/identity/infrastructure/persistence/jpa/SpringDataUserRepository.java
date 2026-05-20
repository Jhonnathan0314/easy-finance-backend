package com.easyfinance.identity.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmail(String email);
}

