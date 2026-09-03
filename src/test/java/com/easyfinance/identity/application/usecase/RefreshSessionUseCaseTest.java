package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.RefreshTokenPort;
import com.easyfinance.identity.application.port.out.TokenIssuerPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.application.response.RotatedRefreshToken;
import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshSessionUseCaseTest {

    private final RefreshTokenPort refreshTokenPort = mock(RefreshTokenPort.class);
    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final ParticipantRepositoryPort participantRepository = mock(ParticipantRepositoryPort.class);
    private final TokenIssuerPort tokenIssuer = mock(TokenIssuerPort.class);
    private final RefreshSessionUseCase useCase = new RefreshSessionUseCase(refreshTokenPort, userRepository, participantRepository, tokenIssuer);

    @Test
    void refreshesSessionForActiveUser() {
        Instant expiresAt = Instant.now().plusSeconds(2592000);
        when(refreshTokenPort.rotate("old-token")).thenReturn(new RotatedRefreshToken("new-token", expiresAt, 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", UserStatus.ACTIVE, Set.of(GlobalRoleName.USER))
        ));
        when(participantRepository.findByUserId(1L)).thenReturn(Optional.of(Participant.restore(20L, 1L, "Jane Doe", ParticipantStatus.ACTIVE)));
        when(tokenIssuer.issueToken(any())).thenReturn("new-access-token");
        when(tokenIssuer.expiresInSeconds()).thenReturn(3600L);

        var result = useCase.refreshSession("old-token");

        assertThat(result.tokenResponse().accessToken()).isEqualTo("new-access-token");
        assertThat(result.tokenResponse().user().participantId()).isEqualTo(20L);
        assertThat(result.refreshToken()).isEqualTo("new-token");
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void rejectsBlankRefreshToken() {
        assertThatThrownBy(() -> useCase.refreshSession(""))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("INVALID_REFRESH_TOKEN"));
        assertThatThrownBy(() -> useCase.refreshSession(null))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void propagatesRotationFailure() {
        when(refreshTokenPort.rotate("bad-token")).thenThrow(new UnauthorizedOperationException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired."));

        assertThatThrownBy(() -> useCase.refreshSession("bad-token"))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("REFRESH_TOKEN_EXPIRED"));
    }

    @Test
    void failsWhenUserNoLongerExists() {
        when(refreshTokenPort.rotate("old-token")).thenReturn(new RotatedRefreshToken("new-token", Instant.now().plusSeconds(2592000), 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.refreshSession("old-token"))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("USER_NOT_FOUND"));
    }

    @Test
    void failsWhenUserIsBlocked() {
        when(refreshTokenPort.rotate("old-token")).thenReturn(new RotatedRefreshToken("new-token", Instant.now().plusSeconds(2592000), 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.restore(1L, "jane@example.com", "$2a$hash", "Jane Doe", UserStatus.BLOCKED, Set.of(GlobalRoleName.USER))
        ));

        assertThatThrownBy(() -> useCase.refreshSession("old-token"))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("USER_NOT_ALLOWED_TO_LOGIN"));
    }
}
