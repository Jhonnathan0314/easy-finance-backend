package com.easyfinance.identity.application.response;

import java.time.Instant;

public record RotatedRefreshToken(String rawToken, Instant expiresAt, Long userId) {
}
