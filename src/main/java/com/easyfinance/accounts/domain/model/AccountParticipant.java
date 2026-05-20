package com.easyfinance.accounts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.Instant;
import java.util.Objects;

public final class AccountParticipant {

    private final Long id;
    private final Long accountId;
    private final Long participantId;
    private final AccountParticipantRole role;
    private final AccountParticipantStatus status;
    private final Instant joinedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private AccountParticipant(
            Long id,
            Long accountId,
            Long participantId,
            AccountParticipantRole role,
            AccountParticipantStatus status,
            Instant joinedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.accountId = requireId(accountId, "ACCOUNT_ID_REQUIRED", "Account id is required.");
        this.participantId = requireId(participantId, "PARTICIPANT_ID_REQUIRED", "Participant id is required.");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AccountParticipant createAdmin(Long accountId, Long participantId) {
        return create(accountId, participantId, AccountParticipantRole.ACCOUNT_ADMIN);
    }

    public static AccountParticipant create(Long accountId, Long participantId, AccountParticipantRole role) {
        return new AccountParticipant(null, accountId, participantId, role, AccountParticipantStatus.ACTIVE, Instant.now(), null, null);
    }

    public static AccountParticipant restore(
            Long id,
            Long accountId,
            Long participantId,
            AccountParticipantRole role,
            AccountParticipantStatus status,
            Instant joinedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AccountParticipant(id, accountId, participantId, role, status, joinedAt, createdAt, updatedAt);
    }

    public AccountParticipant activate(AccountParticipantRole newRole) {
        return new AccountParticipant(id, accountId, participantId, newRole, AccountParticipantStatus.ACTIVE, joinedAt == null ? Instant.now() : joinedAt, createdAt, updatedAt);
    }

    public AccountParticipant changeRole(AccountParticipantRole newRole) {
        return new AccountParticipant(id, accountId, participantId, newRole, status, joinedAt, createdAt, updatedAt);
    }

    public AccountParticipant deactivate() {
        return new AccountParticipant(id, accountId, participantId, role, AccountParticipantStatus.INACTIVE, joinedAt, createdAt, updatedAt);
    }

    public boolean isActiveAdmin() {
        return status == AccountParticipantStatus.ACTIVE && role == AccountParticipantRole.ACCOUNT_ADMIN;
    }

    public Long id() {
        return id;
    }

    public Long accountId() {
        return accountId;
    }

    public Long participantId() {
        return participantId;
    }

    public AccountParticipantRole role() {
        return role;
    }

    public AccountParticipantStatus status() {
        return status;
    }

    public Instant joinedAt() {
        return joinedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Long requireId(Long value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }
}
