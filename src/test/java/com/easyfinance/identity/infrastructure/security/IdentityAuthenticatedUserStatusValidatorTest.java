package com.easyfinance.identity.infrastructure.security;

import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.infrastructure.security.JwtAccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityAuthenticatedUserStatusValidatorTest {

    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final ParticipantRepositoryPort participantRepository = mock(ParticipantRepositoryPort.class);
    private final IdentityAuthenticatedUserStatusValidator validator = new IdentityAuthenticatedUserStatusValidator(userRepository, participantRepository);

    @Test
    void activeUserAndParticipantAreAccepted() {
        givenIdentity(UserStatus.ACTIVE, ParticipantStatus.ACTIVE);

        assertThatCode(() -> validator.validate(currentUser()))
                .doesNotThrowAnyException();
    }

    @Test
    void blockedUserIsRejected() {
        givenIdentity(UserStatus.BLOCKED, ParticipantStatus.ACTIVE);

        assertThatThrownBy(() -> validator.validate(currentUser()))
                .isInstanceOfSatisfying(JwtAccessDeniedException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.code()).isEqualTo("USER_BLOCKED"));
    }

    @Test
    void inactiveUserIsRejected() {
        givenIdentity(UserStatus.INACTIVE, ParticipantStatus.ACTIVE);

        assertThatThrownBy(() -> validator.validate(currentUser()))
                .isInstanceOfSatisfying(JwtAccessDeniedException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.code()).isEqualTo("USER_NOT_ACTIVE"));
    }

    @Test
    void inactiveParticipantIsRejected() {
        givenIdentity(UserStatus.ACTIVE, ParticipantStatus.INACTIVE);

        assertThatThrownBy(() -> validator.validate(currentUser()))
                .isInstanceOfSatisfying(JwtAccessDeniedException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.code()).isEqualTo("PARTICIPANT_NOT_ACTIVE"));
    }

    private void givenIdentity(UserStatus userStatus, ParticipantStatus participantStatus) {
        User user = User.restore(1L, "jane@example.com", "hash", "Jane Doe", userStatus, Set.of(GlobalRoleName.USER));
        Participant participant = Participant.restore(2L, 1L, "Jane Doe", participantStatus);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(participantRepository.findByUserId(1L)).thenReturn(Optional.of(participant));
    }

    private static CurrentUser currentUser() {
        return new CurrentUser(1L, 2L, "jane@example.com", Set.of("USER"), true);
    }
}
