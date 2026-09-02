package com.easyfinance.identity.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipantTest {

    @Test
    void createsActiveParticipantForUser() {
        Participant participant = Participant.createForUser(10L, "Jane Doe");

        assertThat(participant.id()).isNull();
        assertThat(participant.userId()).isEqualTo(10L);
        assertThat(participant.displayName()).isEqualTo("Jane Doe");
        assertThat(participant.status()).isEqualTo(ParticipantStatus.ACTIVE);
    }

    @Test
    void rejectsMissingUserId() {
        assertThatThrownBy(() -> Participant.createForUser(null, "Jane Doe"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("User id is required.");
    }

    @Test
    void renameUpdatesDisplayNameAndKeepsOtherFields() {
        Participant participant = Participant.restore(10L, 1L, "Jane Doe", ParticipantStatus.ACTIVE);

        Participant renamed = participant.rename(" Jane Smith ");

        assertThat(renamed.id()).isEqualTo(10L);
        assertThat(renamed.userId()).isEqualTo(1L);
        assertThat(renamed.displayName()).isEqualTo("Jane Smith");
        assertThat(renamed.status()).isEqualTo(ParticipantStatus.ACTIVE);
    }

    @Test
    void renameRejectsBlankDisplayName() {
        Participant participant = Participant.restore(10L, 1L, "Jane Doe", ParticipantStatus.ACTIVE);

        assertThatThrownBy(() -> participant.rename("   "))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DISPLAY_NAME_REQUIRED"));
    }
}
