package com.easyfinance.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "easy-finance.security.refresh-token")
public record RefreshTokenProperties(
        Duration expiration,
        boolean cookieSecure
) {
}
