package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.command.LoginCommand;
import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.PasswordHasherPort;
import com.easyfinance.identity.application.port.out.TokenIssuerPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginUseCaseTest {

    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final ParticipantRepositoryPort participantRepository = mock(ParticipantRepositoryPort.class);
    private final PasswordHasherPort passwordHasher = mock(PasswordHasherPort.class);
    private final TokenIssuerPort tokenIssuer = mock(TokenIssuerPort.class);
    private final LoginUseCase useCase = new LoginUseCase(userRepository, participantRepository, passwordHasher, tokenIssuer);

    @Test
    void logsInActiveUser() {
        User user = User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", UserStatus.ACTIVE, Set.of(GlobalRoleName.USER));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("abc12345", "$2a$hash")).thenReturn(true);
        when(participantRepository.findByUserId(1L)).thenReturn(Optional.of(Participant.restore(20L, 1L, "Jane Doe", ParticipantStatus.ACTIVE)));
        when(tokenIssuer.issueToken(any())).thenReturn("token");
        when(tokenIssuer.expiresInSeconds()).thenReturn(3600L);

        var response = useCase.login(new LoginCommand("Jane@Example.com", "abc12345"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.user().email()).isEqualTo("jane@example.com");
        assertThat(response.user().participantId()).isEqualTo(20L);
    }

    @Test
    void failsWithGenericErrorWhenPasswordIsWrong() {
        User user = User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", UserStatus.ACTIVE, Set.of(GlobalRoleName.USER));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(new LoginCommand("jane@example.com", "wrong")))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void failsWhenUserIsBlocked() {
        User user = User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", UserStatus.BLOCKED, Set.of(GlobalRoleName.USER));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("abc12345", "$2a$hash")).thenReturn(true);

        assertThatThrownBy(() -> useCase.login(new LoginCommand("jane@example.com", "abc12345")))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessage("User cannot login.");
    }
}
