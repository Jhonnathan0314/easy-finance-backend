package com.easyfinance.shared.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("prod")
class ProdJwtPropertiesValidator {

    private static final String INSECURE_DEFAULT_SECRET = "change-me-in-production";
    private static final String LOCAL_DEFAULT_SECRET = "local-development-jwt-secret-32bytes-minimum";

    private final JwtProperties jwtProperties;

    ProdJwtPropertiesValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void validate() {
        String secret = jwtProperties.secret();
        if (secret == null
                || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || INSECURE_DEFAULT_SECRET.equals(secret)
                || LOCAL_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT_SECRET must be configured with a secure value when the prod profile is active.");
        }
    }
}
