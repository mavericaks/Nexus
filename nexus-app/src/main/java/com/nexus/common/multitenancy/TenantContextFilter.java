package com.nexus.common.multitenancy;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servlet filter that extracts the tenant ID and stores it in
 * {@link TenantContext} for the duration of the request.
 *
 * <p><b>Tenant ID sources (in order of precedence):</b></p>
 * <ol>
 *   <li><b>JWT claim</b> — if the user is authenticated, the JWT contains
 *       a {@code tenantId} claim set during login. This is authoritative.</li>
 *   <li><b>URL path</b> — {@code /api/v1/tenants/{tenantId}/...}.
 *       Must match the JWT claim if both are present.</li>
 * </ol>
 *
 * <p><b>Why validate both?</b> A malicious client could authenticate as
 * tenant A (JWT says tenant A) but request {@code /tenants/B/tickets}.
 * Without this check, the URL tenant would be set in RLS context,
 * potentially leaking tenant B's data. Cross-tenant validation prevents this.</p>
 *
 * <p>Runs AFTER the Spring Security filter chain ({@code @Order(0)}) so that
 * the JWT has been validated and the SecurityContext is populated before
 * this filter reads the tenant claim. Spring Security's filter chain runs
 * at order -100 by default.</p>
 */
@Component
@Order(0)
public class TenantContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    /**
     * Captures the UUID segment after /api/v1/tenants/ in the URL path.
     * Group 1 = the tenant ID.
     */
    private static final Pattern TENANT_URL_PATTERN =
            Pattern.compile("/api/v[0-9]+/tenants/([^/]+)");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String urlTenantId = extractTenantIdFromUrl(httpRequest.getRequestURI());
                String jwtTenantId = extractTenantIdFromJwt();

                if (jwtTenantId != null && urlTenantId != null) {
                    // Both present — they MUST match (cross-tenant attack prevention)
                    if (!jwtTenantId.equals(urlTenantId)) {
                        log.warn("Cross-tenant access attempt: JWT tenant={}, URL tenant={}",
                                jwtTenantId, urlTenantId);
                        ((HttpServletResponse) response).sendError(
                                HttpServletResponse.SC_FORBIDDEN,
                                "Tenant mismatch: your account belongs to a different tenant.");
                        return;
                    }
                    TenantContext.setTenantId(jwtTenantId);
                    log.debug("Tenant context set from JWT + URL: {}", jwtTenantId);
                } else if (jwtTenantId != null) {
                    // JWT only (e.g., endpoints without tenantId in URL)
                    TenantContext.setTenantId(jwtTenantId);
                    log.debug("Tenant context set from JWT: {}", jwtTenantId);
                } else if (urlTenantId != null) {
                    // URL only (e.g., unauthenticated endpoints — shouldn't happen
                    // after Phase 3, but kept for backward compatibility)
                    TenantContext.setTenantId(urlTenantId);
                    log.debug("Tenant context set from URL: {}", urlTenantId);
                }
            }
            chain.doFilter(request, response);
        } finally {
            // CRITICAL: always clear to prevent tenant leakage between
            // requests on the same Tomcat thread.
            TenantContext.clear();
        }
    }

    /**
     * Extracts and validates the tenant ID from the request URI.
     *
     * @return the tenant ID as a string, or null if the URL doesn't
     *         match or the ID isn't a valid UUID.
     */
    private String extractTenantIdFromUrl(String uri) {
        Matcher matcher = TENANT_URL_PATTERN.matcher(uri);
        if (matcher.find()) {
            String candidate = matcher.group(1);
            try {
                // Validate it's a real UUID — prevents SQL injection
                // and garbage tenant IDs from reaching the database.
                UUID.fromString(candidate);
                return candidate;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tenant ID in URL: {}", candidate);
                return null;
            }
        }
        return null;
    }

    /**
     * Extracts the tenant ID from the authenticated JWT's claims.
     *
     * @return the tenant ID as a string, or null if not authenticated
     *         or the JWT doesn't contain a tenantId claim.
     */
    private String extractTenantIdFromJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tenantId = jwt.getClaimAsString("tenantId");
            if (tenantId != null) {
                try {
                    UUID.fromString(tenantId);
                    return tenantId;
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tenant ID in JWT: {}", tenantId);
                    return null;
                }
            }
        }
        return null;
    }
}
