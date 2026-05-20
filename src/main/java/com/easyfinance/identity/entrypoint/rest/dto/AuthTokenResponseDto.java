package com.easyfinance.identity.entrypoint.rest.dto;

public record AuthTokenResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUserDto user
) {
}
