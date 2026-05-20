package com.easyfinance.identity.application.response;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUserResponse user
) {
}

