package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.identity.application.port.out.TokenIssuerPort;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import com.easyfinance.shared.application.CurrentUser;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenService implements TokenIssuerPort {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final MacAlgorithm MAC_ALGORITHM = MacAlgorithm.HS256;

    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Clock clock;

    @Autowired
    public JwtTokenService(JwtProperties jwtProperties) {
        this(jwtProperties, Clock.systemUTC());
    }

    JwtTokenService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        byte[] secret = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256.");
        }
        SecretKey secretKey = new SecretKeySpec(secret, HMAC_SHA256);
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MAC_ALGORITHM)
                .build();
        decoder.setJwtValidator(new JwtIssuerValidator(jwtProperties.issuer()));
        this.jwtDecoder = decoder;
    }

    @Override
    public String issueToken(AuthenticatedUserResponse user) {
        Instant now = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.userId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.expiration()))
                .claim("userId", user.userId())
                .claim("participantId", user.participantId())
                .claim("email", user.email())
                .claim("globalRoles", user.globalRoles())
                .build();

        JwsHeader header = JwsHeader.with(MAC_ALGORITHM)
                .type("JWT")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public long expiresInSeconds() {
        return jwtProperties.expiration().toSeconds();
    }

    public CurrentUser validate(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            validateExpiration(jwt);
            Long userId = jwt.getClaim("userId");
            Long participantId = jwt.getClaim("participantId");
            String email = jwt.getClaimAsString("email");
            Set<String> roles = roles(jwt);

            if (userId == null || participantId == null || email == null || roles.isEmpty()) {
                throw invalidToken();
            }
            return new CurrentUser(userId, participantId, email, roles, true);
        } catch (JwtValidationException ex) {
            throw invalidToken();
        } catch (JwtAuthenticationException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw invalidToken();
        }
    }

    private void validateExpiration(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw invalidToken();
        }
        if (!Instant.now(clock).isBefore(expiresAt)) {
            throw new JwtAuthenticationException("TOKEN_EXPIRED", "Token has expired.");
        }
    }

    private static Set<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("globalRoles");
        if (roles == null) {
            return Set.of();
        }
        return roles.stream().collect(Collectors.toUnmodifiableSet());
    }

    private static JwtAuthenticationException invalidToken() {
        return new JwtAuthenticationException("INVALID_TOKEN", "Invalid token.");
    }
}
