package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetCurrentUserUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final ParticipantRepositoryPort participantRepository = mock(ParticipantRepositoryPort.class);
    private final GetCurrentUserUseCase useCase = new GetCurrentUserUseCase(currentUserProvider, userRepository, participantRepository);

    @Test
    void returnsCurrentUserWhenUserAndParticipantAreActive() {
        givenCurrentUser(UserStatus.ACTIVE, ParticipantStatus.ACTIVE);

        var response = useCase.getCurrentUser();

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.participantId()).isEqualTo(10L);
        assertThat(response.globalRoles()).containsExactly("USER");
    }

    @Test
    void rejectsBlockedUser() {
        givenCurrentUser(UserStatus.BLOCKED, ParticipantStatus.ACTIVE);

        assertThatThrownBy(useCase::getCurrentUser)
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("USER_BLOCKED"));
    }

    @Test
    void rejectsInactiveUser() {
        givenCurrentUser(UserStatus.INACTIVE, ParticipantStatus.ACTIVE);

        assertThatThrownBy(useCase::getCurrentUser)
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("USER_NOT_ACTIVE"));
    }

    @Test
    void rejectsInactiveParticipant() {
        givenCurrentUser(UserStatus.ACTIVE, ParticipantStatus.INACTIVE);

        assertThatThrownBy(useCase::getCurrentUser)
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("PARTICIPANT_NOT_ACTIVE"));
    }

    private void givenCurrentUser(UserStatus userStatus, ParticipantStatus participantStatus) {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "jane@example.com", Set.of("USER"), true)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", userStatus, Set.of(GlobalRoleName.USER))));
        when(participantRepository.findByUserId(1L)).thenReturn(Optional.of(Participant.restore(10L, 1L, "Jane Doe", participantStatus)));
    }
}
