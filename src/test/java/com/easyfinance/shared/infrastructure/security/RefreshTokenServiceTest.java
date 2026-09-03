package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.identity.application.response.IssuedRefreshToken;
import com.easyfinance.identity.application.response.RotatedRefreshToken;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private final SpringDataRefreshTokenRepository repository = mock(SpringDataRefreshTokenRepository.class);
    private final RefreshTokenProperties properties = new RefreshTokenProperties(Duration.ofDays(30), true);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-11T10:00:00Z"), ZoneOffset.UTC);
    private final RefreshTokenService service = new RefreshTokenService(repository, properties, clock);

    @Test
    void issueSavesANewRowWithAFreshFamily() {
        ArgumentCaptor<RefreshTokenJpaEntity> captor = ArgumentCaptor.forClass(RefreshTokenJpaEntity.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken issued = service.issue(1L);

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.expiresAt()).isEqualTo(Instant.parse("2026-06-10T10:00:00Z"));
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getFamilyId()).isNotNull();
        assertThat(captor.getValue().getTokenHash()).isNotBlank();
        assertThat(captor.getValue().getRevokedAt()).isNull();
    }

    @Test
    void rotateRevokesOldRowAndIssuesANewOneInTheSameFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshTokenJpaEntity existing = entity(10L, familyId, null, Instant.parse("2026-06-01T00:00:00Z"));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(repository.save(any(RefreshTokenJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RotatedRefreshToken rotated = service.rotate("some-raw-token");

        assertThat(rotated.userId()).isEqualTo(10L);
        assertThat(rotated.rawToken()).isNotBlank();
        assertThat(existing.getRevokedAt()).isEqualTo(Instant.now(clock));
        assertThat(existing.getReplacedByTokenHash()).isNotBlank();
        verify(repository, times(2)).save(any(RefreshTokenJpaEntity.class));
    }

    @Test
    void rotateWithUnknownTokenFailsWithInvalidRefreshToken() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown-token"))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void rotateWithExpiredTokenFails() {
        UUID familyId = UUID.randomUUID();
        RefreshTokenJpaEntity expired = entity(10L, familyId, null, Instant.parse("2026-05-01T00:00:00Z"));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("expired-token"))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("REFRESH_TOKEN_EXPIRED"));
        verify(repository, never()).save(any());
    }

    @Test
    void rotateWithAlreadyUsedTokenRevokesWholeFamilyAndFails() {
        UUID familyId = UUID.randomUUID();
        RefreshTokenJpaEntity reused = entity(10L, familyId, Instant.parse("2026-05-10T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"));
        RefreshTokenJpaEntity sibling = entity(10L, familyId, null, Instant.parse("2026-06-05T00:00:00Z"));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(reused));
        when(repository.findByFamilyIdAndRevokedAtIsNull(familyId)).thenReturn(List.of(sibling));

        assertThatThrownBy(() -> service.rotate("reused-token"))
                .isInstanceOfSatisfying(UnauthorizedOperationException.class, ex -> assertThat(ex.code()).isEqualTo("REFRESH_TOKEN_REUSED"));

        assertThat(sibling.getRevokedAt()).isEqualTo(Instant.now(clock));
        verify(repository).save(sibling);
    }

    @Test
    void revokeMarksTheMatchingRowRevoked() {
        RefreshTokenJpaEntity existing = entity(10L, UUID.randomUUID(), null, Instant.parse("2026-06-01T00:00:00Z"));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        service.revoke("some-raw-token");

        assertThat(existing.getRevokedAt()).isEqualTo(Instant.now(clock));
        verify(repository).save(existing);
    }

    @Test
    void revokeWithUnknownTokenIsANoOp() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.revoke("unknown-token");

        verify(repository, never()).save(any());
    }

    private static RefreshTokenJpaEntity entity(Long userId, UUID familyId, Instant revokedAt, Instant expiresAt) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setFamilyId(familyId);
        entity.setTokenHash("existing-hash");
        entity.setIssuedAt(expiresAt.minus(Duration.ofDays(30)));
        entity.setExpiresAt(expiresAt);
        entity.setRevokedAt(revokedAt);
        return entity;
    }
}
