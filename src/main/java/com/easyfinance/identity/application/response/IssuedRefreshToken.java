package com.easyfinance.identity.application.response;

import java.time.Instant;

public record IssuedRefreshToken(String rawToken, Instant expiresAt) {
}
