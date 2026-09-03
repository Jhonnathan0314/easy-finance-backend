package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.identity.application.port.out.RefreshTokenPort;
import com.easyfinance.identity.application.response.IssuedRefreshToken;
import com.easyfinance.identity.application.response.RotatedRefreshToken;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class RefreshTokenService implements RefreshTokenPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SpringDataRefreshTokenRepository repository;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    @Autowired
    public RefreshTokenService(SpringDataRefreshTokenRepository repository, RefreshTokenProperties properties) {
        this(repository, properties, Clock.systemUTC());
    }

    RefreshTokenService(SpringDataRefreshTokenRepository repository, RefreshTokenProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedRefreshToken issue(Long userId) {
        return issue(userId, UUID.randomUUID());
    }

    @Override
    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshTokenJpaEntity current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedOperationException("INVALID_REFRESH_TOKEN", "Refresh token is invalid."));

        if (current.getRevokedAt() != null) {
            revokeFamily(current.getFamilyId());
            throw new UnauthorizedOperationException("REFRESH_TOKEN_REUSED", "Refresh token was already used.");
        }
        if (!Instant.now(clock).isBefore(current.getExpiresAt())) {
            throw new UnauthorizedOperationException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired.");
        }

        IssuedRefreshToken next = issue(current.getUserId(), current.getFamilyId());
        current.setRevokedAt(Instant.now(clock));
        current.setReplacedByTokenHash(hash(next.rawToken()));
        repository.save(current);

        return new RotatedRefreshToken(next.rawToken(), next.expiresAt(), current.getUserId());
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(entity -> {
            entity.setRevokedAt(Instant.now(clock));
            repository.save(entity);
        });
    }

    private IssuedRefreshToken issue(Long userId, UUID familyId) {
        String rawToken = generateRawToken();
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(properties.expiration());

        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setUserId(userId);
        entity.setFamilyId(familyId);
        entity.setTokenHash(hash(rawToken));
        entity.setIssuedAt(now);
        entity.setExpiresAt(expiresAt);
        repository.save(entity);

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    private void revokeFamily(UUID familyId) {
        Instant now = Instant.now(clock);
        repository.findByFamilyIdAndRevokedAtIsNull(familyId).forEach(entity -> {
            entity.setRevokedAt(now);
            repository.save(entity);
        });
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
