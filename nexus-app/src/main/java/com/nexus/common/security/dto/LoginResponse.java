package com.nexus.common.security.dto;

import java.util.List;

/**
 * Outbound DTO returned after successful login.
 *
 * @param token    the signed JWT token string
 * @param email    the user's email
 * @param tenantId the tenant UUID the user belongs to
 * @param roles    the user's roles in this tenant
 */
public record LoginResponse(
        String token,
        String email,
        String tenantId,
        List<String> roles
) {}
