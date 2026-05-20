package com.easyfinance.income.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SpringDataIncomeRepository extends JpaRepository<IncomeJpaEntity, Long>, JpaSpecificationExecutor<IncomeJpaEntity> {
    Optional<IncomeJpaEntity> findByAccountIdAndId(Long accountId, Long id);
}
