package com.nexus.common.security;

import com.nexus.common.security.dto.LoginResponse;
import com.nexus.common.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Demo authentication controller — issues a JWT for the pre-seeded demo user.
 *
 * <p>This controller only registers when {@code nexus.demo.enabled=true} is set.
 * It bypasses password-based authentication entirely and should <b>never</b> be
 * enabled in a real production environment.
 *
 * <p><b>Why a separate controller?</b> Using {@code @ConditionalOnProperty} at
 * the class level ensures the entire bean (and its endpoint) is absent from the
 * application context when demo mode is off. This is safer than a runtime flag
 * check — the endpoint literally doesn't exist.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "nexus.demo.enabled", havingValue = "true")
public class DemoAuthController {

    private static final Logger log = LoggerFactory.getLogger(DemoAuthController.class);

    private static final String DEMO_EMAIL = "demo@nexus.dev";

    /**
     * SQL to load the demo user + roles, bypassing RLS via the auth DataSource.
     */
    private static final String FIND_DEMO_USER_SQL = """
            SELECT u.id, u.tenant_id, u.email, r.role
            FROM users u
            LEFT JOIN user_roles r ON u.id = r.user_id
            WHERE u.email = ?
            """;

    private final DataSource authDataSource;
    private final JwtTokenProvider jwtTokenProvider;

    public DemoAuthController(@Qualifier("authDataSource") DataSource authDataSource,
                              JwtTokenProvider jwtTokenProvider) {
        this.authDataSource = authDataSource;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticate as the demo user — no credentials required.
     *
     * <p>POST /api/v1/auth/demo-login
     *
     * <p>Loads the pre-seeded demo user from the database, generates a JWT,
     * and returns it in the same format as the regular login endpoint.
     */
    @PostMapping("/demo-login")
    public ResponseEntity<LoginResponse> demoLogin() {
        log.info("Demo login requested");

        try (Connection conn = authDataSource.getConnection()) {
            conn.setAutoCommit(true);

            try (PreparedStatement ps = conn.prepareStatement(FIND_DEMO_USER_SQL)) {
                ps.setString(1, DEMO_EMAIL);

                try (ResultSet rs = ps.executeQuery()) {
                    UUID userId = null;
                    UUID tenantId = null;
                    String email = null;
                    List<String> roles = new ArrayList<>();

                    while (rs.next()) {
                        if (userId == null) {
                            userId = UUID.fromString(rs.getString("id"));
                            tenantId = UUID.fromString(rs.getString("tenant_id"));
                            email = rs.getString("email");
                        }
                        String role = rs.getString("role");
                        if (role != null) {
                            roles.add(role);
                        }
                    }

                    if (userId == null) {
                        log.error("Demo user not found in database. Did the seed migration run?");
                        return ResponseEntity.internalServerError().build();
                    }

                    String token = jwtTokenProvider.generateToken(email, userId, tenantId, roles);

                    log.info("Demo login successful: user={}, tenant={}", email, tenantId);

                    return ResponseEntity.ok(new LoginResponse(
                            token,
                            email,
                            tenantId.toString(),
                            roles
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Demo login failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
