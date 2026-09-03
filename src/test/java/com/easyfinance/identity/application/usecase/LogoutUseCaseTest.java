package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.port.out.RefreshTokenPort;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LogoutUseCaseTest {

    private final RefreshTokenPort refreshTokenPort = mock(RefreshTokenPort.class);
    private final LogoutUseCase useCase = new LogoutUseCase(refreshTokenPort);

    @Test
    void revokesThePresentedToken() {
        useCase.logout("some-raw-token");

        verify(refreshTokenPort).revoke("some-raw-token");
    }

    @Test
    void doesNothingWhenTokenIsBlank() {
        useCase.logout(null);
        useCase.logout("");
        useCase.logout("  ");

        verify(refreshTokenPort, never()).revoke(any());
    }
}
