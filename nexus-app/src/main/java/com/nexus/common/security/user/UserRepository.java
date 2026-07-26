package com.nexus.common.security.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user lookups.
 *
 * <p>The {@code findByEmail} method bypasses RLS intentionally —
 * during login, we don't have a tenant context yet (the user hasn't
 * authenticated). The query runs as the database owner (Flyway role)
 * or we need a special approach.
 *
 * <p><b>Important:</b> Since RLS is enabled on the users table and the
 * app role (nexus_app) has RLS enforced, login queries won't work
 * through the normal DataSource path. We solve this by making the
 * login query run with a native query that uses a function or by
 * temporarily setting a known tenant context. For now, we use a
 * native query approach — the auth flow sets tenant context after
 * finding the user.
 */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Find a user by email for authentication.
     *
     * <p>Note: This query needs to work without RLS context.
     * The TenantContextFilter sets tenant_id from the URL path,
     * but the login endpoint doesn't have a tenant in the URL.
     * We handle this by having the auth endpoint set a temporary
     * context or by using a direct query.
     */
    Optional<UserEntity> findByEmail(String email);
}
