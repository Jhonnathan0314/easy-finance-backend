package com.easyfinance.accounts.application.service;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignedParticipantValidatorTest {

    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final AssignedParticipantValidator validator = new AssignedParticipantValidator(accountParticipantRepository);

    @Test
    void fallsBackToActorParticipantWhenRequestIsNull() {
        Long resolved = validator.resolveAssignedParticipantId(access(AccountParticipantRole.ACCOUNT_MEMBER, 10L), null);

        assertThat(resolved).isEqualTo(10L);
        verify(accountParticipantRepository, never()).findByAccountIdAndParticipantId(1L, 10L);
    }

    @Test
    void nullableResolutionKeepsNullWhenRequestIsNull() {
        Long resolved = validator.resolveNullableAssignedParticipantId(access(AccountParticipantRole.ACCOUNT_MEMBER, 10L), null);

        assertThat(resolved).isNull();
        verify(accountParticipantRepository, never()).findByAccountIdAndParticipantId(1L, 10L);
    }

    @Test
    void allowsMemberToAssignSelfWithoutExtraLookup() {
        Long resolved = validator.resolveAssignedParticipantId(access(AccountParticipantRole.ACCOUNT_MEMBER, 10L), 10L);

        assertThat(resolved).isEqualTo(10L);
        verify(accountParticipantRepository, never()).findByAccountIdAndParticipantId(1L, 10L);
    }

    @Test
    void blocksMemberAssigningAnotherParticipant() {
        assertThatThrownBy(() -> validator.resolveAssignedParticipantId(access(AccountParticipantRole.ACCOUNT_MEMBER, 10L), 20L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ASSIGNED_PARTICIPANT_NOT_ALLOWED"));
    }

    @Test
    void allowsAdminAssigningActiveAccountParticipant() {
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 20L))
                .thenReturn(Optional.of(membership(AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, 20L)));

        Long resolved = validator.resolveAssignedParticipantId(access(AccountParticipantRole.ACCOUNT_ADMIN, 10L), 20L);

        assertThat(resolved).isEqualTo(20L);
    }

    @Test
    void rejectsAssignedParticipantOutsideAccount() {
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.resolveAssignedParticipantId(access(AccountParticipantRole.ACCOUNT_ADMIN, 10L), 20L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("ASSIGNED_PARTICIPANT_NOT_FOUND"));
    }

    @Test
    void rejectsInactiveAssignedParticipant() {
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 20L))
                .thenReturn(Optional.of(membership(AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.INACTIVE, 20L)));

        assertThatThrownBy(() -> validator.resolveAssignedParticipantId(access(AccountParticipantRole.ACCOUNT_ADMIN, 10L), 20L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ASSIGNED_PARTICIPANT_NOT_ACTIVE"));
    }

    private static AccountAccess access(AccountParticipantRole role, Long participantId) {
        return new AccountAccess(
                Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now()),
                membership(role, AccountParticipantStatus.ACTIVE, participantId)
        );
    }

    private static AccountParticipant membership(AccountParticipantRole role, AccountParticipantStatus status, Long participantId) {
        return AccountParticipant.restore(participantId, 1L, participantId, role, status, Instant.now(), null, null);
    }
}
