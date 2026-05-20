package com.easyfinance.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "easy-finance.security.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration expiration
) {
}

