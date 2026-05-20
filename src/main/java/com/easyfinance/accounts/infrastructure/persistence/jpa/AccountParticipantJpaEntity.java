package com.easyfinance.accounts.infrastructure.persistence.jpa;

import com.easyfinance.shared.infrastructure.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "account_participants")
public class AccountParticipantJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountJpaEntity account;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private AccountParticipantRoleJpa role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountParticipantStatusJpa status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountJpaEntity getAccount() {
        return account;
    }

    public void setAccount(AccountJpaEntity account) {
        this.account = account;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    public AccountParticipantRoleJpa getRole() {
        return role;
    }

    public void setRole(AccountParticipantRoleJpa role) {
        this.role = role;
    }

    public AccountParticipantStatusJpa getStatus() {
        return status;
    }

    public void setStatus(AccountParticipantStatusJpa status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
