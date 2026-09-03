package com.easyfinance.identity.application.response;

import java.time.Instant;

public record AuthSessionResult(
        AuthTokenResponse tokenResponse,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
