package com.easyfinance.accounts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountParticipantTest {

    @Test
    void createsAdminMembership() {
        AccountParticipant membership = AccountParticipant.createAdmin(1L, 10L);

        assertThat(membership.accountId()).isEqualTo(1L);
        assertThat(membership.participantId()).isEqualTo(10L);
        assertThat(membership.role()).isEqualTo(AccountParticipantRole.ACCOUNT_ADMIN);
        assertThat(membership.status()).isEqualTo(AccountParticipantStatus.ACTIVE);
    }

    @Test
    void createsMemberMembership() {
        AccountParticipant membership = AccountParticipant.create(1L, 10L, AccountParticipantRole.ACCOUNT_MEMBER);

        assertThat(membership.role()).isEqualTo(AccountParticipantRole.ACCOUNT_MEMBER);
        assertThat(membership.status()).isEqualTo(AccountParticipantStatus.ACTIVE);
    }

    @Test
    void rejectsMissingParticipant() {
        assertThatThrownBy(() -> AccountParticipant.create(1L, null, AccountParticipantRole.ACCOUNT_MEMBER))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Participant id is required.");
    }

    @Test
    void changesRoleAndDeactivates() {
        AccountParticipant membership = AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE, Instant.now(), null, null);

        assertThat(membership.changeRole(AccountParticipantRole.ACCOUNT_MEMBER).role()).isEqualTo(AccountParticipantRole.ACCOUNT_MEMBER);
        assertThat(membership.deactivate().status()).isEqualTo(AccountParticipantStatus.INACTIVE);
    }
}
