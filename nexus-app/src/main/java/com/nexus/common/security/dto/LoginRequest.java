package com.nexus.common.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO for the login endpoint.
 *
 * @param email    the user's email
 * @param password the user's plain-text password (validated against BCrypt hash)
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
