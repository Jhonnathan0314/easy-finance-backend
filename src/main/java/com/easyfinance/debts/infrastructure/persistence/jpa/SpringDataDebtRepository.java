package com.easyfinance.debts.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SpringDataDebtRepository extends JpaRepository<DebtJpaEntity, Long>, JpaSpecificationExecutor<DebtJpaEntity> {

    Optional<DebtJpaEntity> findByAccountIdAndId(Long accountId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DebtJpaEntity d where d.accountId = :accountId and d.id = :id")
    Optional<DebtJpaEntity> findByAccountIdAndIdForUpdate(@Param("accountId") Long accountId, @Param("id") Long id);
}
