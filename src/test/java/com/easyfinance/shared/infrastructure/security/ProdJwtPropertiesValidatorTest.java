package com.easyfinance.shared.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdJwtPropertiesValidatorTest {

    @Test
    void rejectsBlankSecret() {
        ProdJwtPropertiesValidator validator = new ProdJwtPropertiesValidator(
                new JwtProperties("easy-finance", " ", Duration.ofHours(1))
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be configured with a secure value when the prod profile is active.");
    }

    @Test
    void rejectsInsecureDefaultSecret() {
        ProdJwtPropertiesValidator validator = new ProdJwtPropertiesValidator(
                new JwtProperties("easy-finance", "change-me-in-production", Duration.ofHours(1))
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be configured with a secure value when the prod profile is active.");
    }

    @Test
    void acceptsConfiguredSecret() {
        ProdJwtPropertiesValidator validator = new ProdJwtPropertiesValidator(
                new JwtProperties("easy-finance", "a-secure-secret-for-tests-with-32-bytes", Duration.ofHours(1))
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
