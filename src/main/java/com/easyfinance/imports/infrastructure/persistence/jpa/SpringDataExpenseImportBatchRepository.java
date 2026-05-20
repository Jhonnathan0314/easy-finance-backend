package com.easyfinance.imports.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SpringDataExpenseImportBatchRepository extends JpaRepository<ExpenseImportBatchJpaEntity, Long> {

    Optional<ExpenseImportBatchJpaEntity> findByAccountIdAndId(Long accountId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ExpenseImportBatchJpaEntity b where b.accountId = :accountId and b.id = :id")
    Optional<ExpenseImportBatchJpaEntity> findByAccountIdAndIdForUpdate(@Param("accountId") Long accountId, @Param("id") Long id);
}
