package com.nexus.common.security.user;

/**
 * RBAC roles for Nexus users.
 *
 * <p>Prefixed with {@code ROLE_} to match Spring Security's convention.
 * Spring Security's {@code hasRole("ADMIN")} automatically prepends
 * {@code ROLE_}, so we store them with the prefix to avoid confusion.
 *
 * <ul>
 *   <li>{@code ROLE_OWNER} — tenant owner, full access including billing</li>
 *   <li>{@code ROLE_ADMIN} — can manage users, tickets, and settings</li>
 *   <li>{@code ROLE_AGENT} — can view and work on tickets only</li>
 * </ul>
 */
public enum UserRole {
    ROLE_OWNER,
    ROLE_ADMIN,
    ROLE_AGENT
}
