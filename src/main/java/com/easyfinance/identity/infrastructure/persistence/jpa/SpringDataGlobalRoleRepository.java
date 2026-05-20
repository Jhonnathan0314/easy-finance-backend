package com.easyfinance.identity.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataGlobalRoleRepository extends JpaRepository<GlobalRoleJpaEntity, Long> {

    Optional<GlobalRoleJpaEntity> findByName(GlobalRoleNameJpa name);
}

