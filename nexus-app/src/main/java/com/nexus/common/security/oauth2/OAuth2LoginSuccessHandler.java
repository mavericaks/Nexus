package com.nexus.common.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the callback after Google authenticates a user.
 *
 * <p><b>What happens here (the "token exchange"):</b></p>
 * <ol>
 *   <li>Google authenticates the user and redirects back with an auth code</li>
 *   <li>Spring Security exchanges the code for Google tokens + user info (OIDC)</li>
 *   <li>This handler receives the authenticated {@link OidcUser} with Google's
 *       email, name, picture, etc.</li>
 *   <li>We look up the Google email in our {@code users} table:
 *       <ul>
 *         <li>If found → issue our JWT with the existing user's tenant/roles</li>
 *         <li>If not found → return 403 (account must exist; admins pre-create users)</li>
 *       </ul>
 *   </li>
 *   <li>Return the JWT as JSON — the frontend stores it for subsequent API calls</li>
 * </ol>
 *
 * <p><b>Why not auto-create users on first Google login?</b></p>
 * <p>In B2B SaaS, tenant admins control who has access. A random Google user
 * shouldn't get an account just because they clicked "Sign in with Google."
 * The admin pre-creates users (with email + role), then the user can sign in
 * via Google. This is the "account linking" pattern.</p>
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtTokenProvider jwtTokenProvider;
    private final DataSource authDataSource;

    /**
     * SQL to find an existing user by email + load their roles.
     * Uses the authDataSource (DB owner) to bypass RLS.
     */
    private static final String FIND_USER_SQL = """
            SELECT u.id, u.tenant_id, u.email, u.name, u.enabled, r.role
            FROM users u
            LEFT JOIN user_roles r ON u.id = r.user_id
            WHERE u.email = ?
            """;

    public OAuth2LoginSuccessHandler(JwtTokenProvider jwtTokenProvider,
                                     @Qualifier("authDataSource") DataSource authDataSource) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authDataSource = authDataSource;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String googleEmail = oidcUser.getEmail();
        String googleName = oidcUser.getFullName();

        log.info("OAuth2 login: email={}, name={}", googleEmail, googleName);

        try {
            UserLookupResult user = findUserByEmail(googleEmail);

            if (user == null) {
                // User doesn't exist in our system — reject
                log.warn("OAuth2 login rejected: no Nexus account for email={}", googleEmail);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "status", 403,
                        "error", "Forbidden",
                        "message", "No Nexus account linked to " + googleEmail
                                + ". Ask your tenant admin to create your account first."
                )));
                return;
            }

            if (!user.enabled()) {
                log.warn("OAuth2 login rejected: account disabled for email={}", googleEmail);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "status", 403,
                        "error", "Forbidden",
                        "message", "Your account has been disabled."
                )));
                return;
            }

            // Issue our own JWT with tenant context + roles
            String token = jwtTokenProvider.generateToken(
                    user.email(), user.userId(), user.tenantId(), user.roles());

            log.info("OAuth2 login success: email={}, tenant={}, roles={}",
                    user.email(), user.tenantId(), user.roles());

            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "token", token,
                    "email", user.email(),
                    "name", googleName != null ? googleName : user.email(),
                    "tenantId", user.tenantId().toString(),
                    "roles", user.roles(),
                    "provider", "google"
            )));

        } catch (SQLException e) {
            log.error("OAuth2 login failed: database error for email={}", googleEmail, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "status", 500,
                    "error", "Internal Server Error",
                    "message", "Authentication failed. Please try again."
            )));
        }
    }

    /**
     * Look up a user by email using direct JDBC (bypasses RLS).
     * Same approach as {@link com.nexus.common.security.NexusUserDetailsService}.
     */
    private UserLookupResult findUserByEmail(String email) throws SQLException {
        try (Connection conn = authDataSource.getConnection()) {
            conn.setAutoCommit(true);

            try (PreparedStatement ps = conn.prepareStatement(FIND_USER_SQL)) {
                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {
                    UUID userId = null;
                    UUID tenantId = null;
                    String userEmail = null;
                    boolean enabled = true;
                    List<String> roles = new ArrayList<>();

                    while (rs.next()) {
                        if (userId == null) {
                            userId = UUID.fromString(rs.getString("id"));
                            tenantId = UUID.fromString(rs.getString("tenant_id"));
                            userEmail = rs.getString("email");
                            enabled = rs.getBoolean("enabled");
                        }
                        String role = rs.getString("role");
                        if (role != null) {
                            roles.add(role);
                        }
                    }

                    if (userId == null) {
                        return null;
                    }

                    return new UserLookupResult(userId, tenantId, userEmail, enabled, roles);
                }
            }
        }
    }

    /**
     * Internal record to hold user lookup results.
     */
    private record UserLookupResult(
            UUID userId,
            UUID tenantId,
            String email,
            boolean enabled,
            List<String> roles
    ) {}
}
