package com.nexus.common.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtTokenProvider}.
 *
 * <p>Creates a real encoder/decoder with a test secret — no mocking needed.
 * Verifies that generated tokens contain the correct claims and can be
 * decoded back.
 */
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hs256!!";
    private static final String TEST_ISSUER = "nexus-test";

    private JwtTokenProvider tokenProvider;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(TEST_SECRET.getBytes(), "HmacSHA256");

        // Build a JWK with explicit HS256 algorithm so the encoder can select it
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(key)
                .keyID("nexus-test")
                .algorithm(JWSAlgorithm.HS256)
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_SECRET);
        properties.setExpirationHours(1);
        properties.setIssuer(TEST_ISSUER);

        tokenProvider = new JwtTokenProvider(encoder, properties);
    }

    @Test
    @DisplayName("generates a valid JWT token that can be decoded")
    void generatesValidToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "admin@acme.com";
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_AGENT");

        String token = tokenProvider.generateToken(email, userId, tenantId, roles);

        // Token should be a non-empty string with 3 Base64 parts (header.payload.signature)
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length, "JWT should have 3 dot-separated parts");
    }

    @Test
    @DisplayName("token contains correct claims: sub, userId, tenantId, roles, iss")
    void tokenContainsCorrectClaims() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "agent@beta.com";
        List<String> roles = List.of("ROLE_AGENT");

        String token = tokenProvider.generateToken(email, userId, tenantId, roles);
        Jwt decoded = jwtDecoder.decode(token);

        assertEquals(email, decoded.getSubject());
        assertEquals(userId.toString(), decoded.getClaimAsString("userId"));
        assertEquals(tenantId.toString(), decoded.getClaimAsString("tenantId"));
        assertEquals(roles, decoded.getClaimAsStringList("roles"));
        assertEquals(TEST_ISSUER, decoded.getClaimAsString("iss"));
    }

    @Test
    @DisplayName("token has correct expiration (issued + expirationHours)")
    void tokenHasExpiration() {
        String token = tokenProvider.generateToken(
                "user@test.com", UUID.randomUUID(), UUID.randomUUID(),
                List.of("ROLE_AGENT"));

        Jwt decoded = jwtDecoder.decode(token);

        assertNotNull(decoded.getIssuedAt());
        assertNotNull(decoded.getExpiresAt());
        assertTrue(decoded.getExpiresAt().isAfter(decoded.getIssuedAt()),
                "Expiration must be after issuedAt");
    }

    @Test
    @DisplayName("different users get different tokens")
    void differentUsersGetDifferentTokens() {
        String token1 = tokenProvider.generateToken(
                "user1@test.com", UUID.randomUUID(), UUID.randomUUID(),
                List.of("ROLE_AGENT"));
        String token2 = tokenProvider.generateToken(
                "user2@test.com", UUID.randomUUID(), UUID.randomUUID(),
                List.of("ROLE_ADMIN"));

        assertNotEquals(token1, token2);
    }
}
