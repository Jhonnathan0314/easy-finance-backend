package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    @Test
    void issuesAndValidatesToken() {
        JwtTokenService service = newService(Clock.systemUTC(), Duration.ofHours(1));
        var user = new AuthenticatedUserResponse(1L, 10L, "jane@example.com", "Jane Doe", Set.of("USER"));

        String token = service.issueToken(user);
        var currentUser = service.validate(token);

        assertThat(currentUser.authenticated()).isTrue();
        assertThat(currentUser.userId()).isEqualTo(1L);
        assertThat(currentUser.participantId()).isEqualTo(10L);
        assertThat(currentUser.email()).isEqualTo("jane@example.com");
        assertThat(currentUser.globalRoles()).containsExactly("USER");
    }

    @Test
    void rejectsInvalidToken() {
        JwtTokenService service = newService(Clock.systemUTC(), Duration.ofHours(1));

        assertThatThrownBy(() -> service.validate("not-a-token"))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("Invalid token.");
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenService issuer = newService(Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC), Duration.ofHours(1));
        String token = issuer.issueToken(new AuthenticatedUserResponse(1L, 10L, "jane@example.com", "Jane Doe", Set.of("USER")));
        JwtTokenService validator = newService(Clock.systemUTC(), Duration.ofHours(1));

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("Token has expired.");
    }

    private static JwtTokenService newService(Clock clock, Duration expiration) {
        return new JwtTokenService(
                new JwtProperties("easy-finance-test", "test-secret-with-enough-entropy-32-bytes", expiration),
                clock
        );
    }
}
