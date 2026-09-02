package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.command.UpdateProfileCommand;
import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateProfileUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final ParticipantRepositoryPort participantRepository = mock(ParticipantRepositoryPort.class);
    private final UpdateProfileUseCase useCase = new UpdateProfileUseCase(currentUserProvider, userRepository, participantRepository);

    @Test
    void updatesFullNameAndDisplayNameToTheSameValue() {
        givenCurrentUser(UserStatus.ACTIVE, ParticipantStatus.ACTIVE);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.updateProfile(new UpdateProfileCommand("Jane Smith"));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.participantId()).isEqualTo(10L);
        assertThat(response.fullName()).isEqualTo("Jane Smith");
        assertThat(response.globalRoles()).containsExactly("USER");

        var userCaptor = ArgumentCaptor.forClass(User.class);
        var participantCaptor = ArgumentCaptor.forClass(Participant.class);
        verify(userRepository).save(userCaptor.capture());
        verify(participantRepository).save(participantCaptor.capture());
        assertThat(userCaptor.getValue().fullName()).isEqualTo("Jane Smith");
        assertThat(participantCaptor.getValue().displayName()).isEqualTo("Jane Smith");
    }

    @Test
    void rejectsBlankFullName() {
        givenCurrentUser(UserStatus.ACTIVE, ParticipantStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.updateProfile(new UpdateProfileCommand("   ")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("FULL_NAME_REQUIRED"));
        verify(userRepository, never()).save(any());
        verify(participantRepository, never()).save(any());
    }

    @Test
    void rejectsUnauthenticatedRequest() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateProfile(new UpdateProfileCommand("Jane Smith")))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("UNAUTHENTICATED"));
    }

    @Test
    void rejectsMissingUser() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "jane@example.com", Set.of("USER"), true)));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateProfile(new UpdateProfileCommand("Jane Smith")))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("USER_NOT_FOUND"));
    }

    @Test
    void rejectsMissingParticipant() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "jane@example.com", Set.of("USER"), true)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));
        when(participantRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateProfile(new UpdateProfileCommand("Jane Smith")))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("PARTICIPANT_NOT_FOUND"));
    }

    @Test
    void rejectsBlockedUser() {
        givenCurrentUser(UserStatus.BLOCKED, ParticipantStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.updateProfile(new UpdateProfileCommand("Jane Smith")))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("USER_BLOCKED"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsInactiveUser() {
        givenCurrentUser(UserStatus.INACTIVE, ParticipantStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.updateProfile(new UpdateProfileCommand("Jane Smith")))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("USER_NOT_ACTIVE"));
    }

    @Test
    void rejectsInactiveParticipant() {
        givenCurrentUser(UserStatus.ACTIVE, ParticipantStatus.INACTIVE);

        assertThatThrownBy(() -> useCase.updateProfile(new UpdateProfileCommand("Jane Smith")))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("PARTICIPANT_NOT_ACTIVE"));
    }

    private void givenCurrentUser(UserStatus userStatus, ParticipantStatus participantStatus) {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "jane@example.com", Set.of("USER"), true)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", userStatus, Set.of(GlobalRoleName.USER))));
        when(participantRepository.findByUserId(1L)).thenReturn(Optional.of(Participant.restore(10L, 1L, "Jane Doe", participantStatus)));
    }

    private static User activeUser() {
        return User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", UserStatus.ACTIVE, Set.of(GlobalRoleName.USER));
    }
}
