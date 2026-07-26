package com.nexus.common.security;

import com.nexus.common.security.user.UserEntity;
import com.nexus.common.security.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loads users from the database for Spring Security authentication.
 *
 * <p><b>Why direct JDBC instead of UserRepository?</b></p>
 * <p>During login, there's no tenant context yet — the user hasn't
 * authenticated, so we don't know their tenant. But RLS on the
 * {@code users} table requires {@code app.tenant_id} to be set.
 * Using {@code UserRepository.findByEmail()} would go through our
 * primary DataSource ({@code nexus_app} role), which has RLS enforced
 * — the query would return zero rows.</p>
 *
 * <p>The fix: use a secondary DataSource ({@code authDataSource}) that
 * connects as the DB owner, bypassing RLS for this one login query.
 * This is safe because we're only reading one user row by email —
 * there's no multi-tenant data to leak.</p>
 */
@Service
public class NexusUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(NexusUserDetailsService.class);

    private final DataSource authDataSource;

    /**
     * SQL to load user + roles in one query via LEFT JOIN.
     * Runs against the owner DataSource, bypassing RLS.
     */
    private static final String FIND_USER_SQL = """
            SELECT u.id, u.tenant_id, u.email, u.password_hash, u.name, u.enabled, r.role
            FROM users u
            LEFT JOIN user_roles r ON u.id = r.user_id
            WHERE u.email = ?
            """;

    public NexusUserDetailsService(@Qualifier("authDataSource") DataSource authDataSource) {
        this.authDataSource = authDataSource;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try (Connection conn = authDataSource.getConnection()) {
            // Use a raw connection — don't go through TenantAwareDataSource's
            // setAutoCommit interception, since we have no tenant context here.
            // The connection uses the owner role, bypassing RLS.
            conn.setAutoCommit(true);

            try (PreparedStatement ps = conn.prepareStatement(FIND_USER_SQL)) {
                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {
                    UUID userId = null;
                    UUID tenantId = null;
                    String userEmail = null;
                    String passwordHash = null;
                    boolean enabled = true;
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                    while (rs.next()) {
                        if (userId == null) {
                            userId = UUID.fromString(rs.getString("id"));
                            tenantId = UUID.fromString(rs.getString("tenant_id"));
                            userEmail = rs.getString("email");
                            passwordHash = rs.getString("password_hash");
                            enabled = rs.getBoolean("enabled");
                        }

                        String role = rs.getString("role");
                        if (role != null) {
                            authorities.add(new SimpleGrantedAuthority(role));
                        }
                    }

                    if (userId == null) {
                        throw new UsernameNotFoundException("User not found: " + email);
                    }

                    if (!enabled) {
                        throw new UsernameNotFoundException("User disabled: " + email);
                    }

                    log.debug("Loaded user: email={}, tenant={}, roles={}",
                            userEmail, tenantId, authorities);

                    return new NexusUserDetails(
                            userId, tenantId, userEmail, passwordHash, authorities);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user: " + email, e);
        }
    }
}
