package com.nexus.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed, validated configuration for JWT token generation.
 *
 * <p>Bound to {@code nexus.security.jwt.*} properties in application YAML.
 * Using {@code @ConfigurationProperties} instead of scattered {@code @Value}
 * gives us IDE autocomplete, type safety, and fail-fast on startup if
 * a required property is missing.
 */
@Component
@ConfigurationProperties(prefix = "nexus.security.jwt")
public class JwtProperties {

    private String secret;
    private int expirationHours = 24;
    private String issuer = "nexus";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpirationHours() {
        return expirationHours;
    }

    public void setExpirationHours(int expirationHours) {
        this.expirationHours = expirationHours;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
