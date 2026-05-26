package com.easyfinance.debts.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SpringDataDebtPaymentRepository extends JpaRepository<DebtPaymentJpaEntity, Long>, JpaSpecificationExecutor<DebtPaymentJpaEntity> {

    Optional<DebtPaymentJpaEntity> findByAccountIdAndDebtIdAndId(Long accountId, Long debtId, Long id);

    boolean existsByAccountIdAndDebtIdAndStatus(Long accountId, Long debtId, DebtPaymentStatusJpa status);
}
