package com.easyfinance.accounts.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SpringDataAccountParticipantRepository extends JpaRepository<AccountParticipantJpaEntity, Long> {

    Optional<AccountParticipantJpaEntity> findByAccountIdAndParticipantId(Long accountId, Long participantId);

    List<AccountParticipantJpaEntity> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ap from AccountParticipantJpaEntity ap
            where ap.account.id = :accountId
            """)
    List<AccountParticipantJpaEntity> lockByAccountId(@Param("accountId") Long accountId);

    long countByAccountIdAndRoleAndStatus(Long accountId, AccountParticipantRoleJpa role, AccountParticipantStatusJpa status);

    @Query("""
            select ap from AccountParticipantJpaEntity ap
            join ap.account account
            where ap.participantId = :participantId
              and ap.status = com.easyfinance.accounts.infrastructure.persistence.jpa.AccountParticipantStatusJpa.ACTIVE
              and account.status <> com.easyfinance.accounts.infrastructure.persistence.jpa.AccountStatusJpa.ARCHIVED
            order by account.name asc
            """)
    Page<AccountParticipantJpaEntity> findMembershipsForParticipant(@Param("participantId") Long participantId, Pageable pageable);
}
