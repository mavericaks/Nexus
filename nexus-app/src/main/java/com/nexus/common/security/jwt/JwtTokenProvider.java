package com.nexus.common.security.jwt;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Creates signed JWT tokens for authenticated users.
 *
 * <p><b>Token anatomy (claims):</b></p>
 * <ul>
 *   <li>{@code sub} — user email (the "subject" — who this token represents)</li>
 *   <li>{@code userId} — the user's UUID (for database lookups)</li>
 *   <li>{@code tenantId} — the tenant this user belongs to (for RLS context)</li>
 *   <li>{@code roles} — list of roles (OWNER, ADMIN, AGENT) for RBAC</li>
 *   <li>{@code iss} — issuer (who created this token)</li>
 *   <li>{@code iat} — issued at (when the token was created)</li>
 *   <li>{@code exp} — expiration (when the token becomes invalid)</li>
 * </ul>
 *
 * <p>The encoder uses HMAC-SHA256 to sign the token — the same key that
 * {@link org.springframework.security.oauth2.jwt.JwtDecoder} uses to verify it.
 */
@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Generate a signed JWT token for the given user.
     *
     * @param email    the user's email (becomes the "sub" claim)
     * @param userId   the user's UUID
     * @param tenantId the tenant the user belongs to
     * @param roles    the user's roles (e.g., ["ROLE_ADMIN", "ROLE_AGENT"])
     * @return a signed JWT token string
     */
    public String generateToken(String email, UUID userId, UUID tenantId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getExpirationHours(), ChronoUnit.HOURS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(email)
                .claim("userId", userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
