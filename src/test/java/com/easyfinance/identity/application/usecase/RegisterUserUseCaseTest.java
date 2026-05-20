package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.command.RegisterUserCommand;
import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.PasswordHasherPort;
import com.easyfinance.identity.application.port.out.TokenIssuerPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserUseCaseTest {

    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final ParticipantRepositoryPort participantRepository = mock(ParticipantRepositoryPort.class);
    private final PasswordHasherPort passwordHasher = mock(PasswordHasherPort.class);
    private final TokenIssuerPort tokenIssuer = mock(TokenIssuerPort.class);
    private final RegisterUserUseCase useCase = new RegisterUserUseCase(userRepository, participantRepository, passwordHasher, tokenIssuer);

    @Test
    void registersUserParticipantAndDefaultRole() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordHasher.hash("abc12345")).thenReturn("$2a$hash");
        when(userRepository.save(any(User.class))).thenReturn(User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", UserStatus.ACTIVE, Set.of(GlobalRoleName.USER)));
        when(participantRepository.save(any(Participant.class))).thenReturn(Participant.restore(20L, 1L, "Jane Doe", ParticipantStatus.ACTIVE));
        when(tokenIssuer.issueToken(any())).thenReturn("token");
        when(tokenIssuer.expiresInSeconds()).thenReturn(3600L);

        var response = useCase.register(new RegisterUserCommand(" Jane@Example.COM ", "abc12345", "Jane Doe"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().userId()).isEqualTo(1L);
        assertThat(response.user().participantId()).isEqualTo(20L);
        assertThat(response.user().globalRoles()).containsExactly("USER");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().passwordHash()).isEqualTo("$2a$hash");
    }

    @Test
    void failsIfEmailAlreadyExists() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(new RegisterUserCommand("jane@example.com", "abc12345", "Jane Doe")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Email is already registered.");
    }

    @Test
    void enforcesPasswordPolicy() {
        assertThatThrownBy(() -> useCase.register(new RegisterUserCommand("jane@example.com", "abcdefgh", "Jane Doe")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Password must have at least 8 characters, one letter, and one number.");
    }
}
