package com.nexus.common.multitenancy;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servlet filter that extracts the tenant ID from the URL path and
 * stores it in {@link TenantContext} for the duration of the request.
 *
 * <p>Matches URLs like {@code /api/v1/tenants/{tenantId}/...}.
 * The tenantId must be a valid UUID — if it isn't, the request
 * proceeds without tenant context (RLS will return zero rows,
 * which is the fail-closed behavior we want).
 *
 * <p>Runs as the FIRST filter ({@code @Order(Ordered.HIGHEST_PRECEDENCE)})
 * so the tenant context is available to everything downstream.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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
                String tenantId = extractTenantId(httpRequest.getRequestURI());
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                    log.debug("Tenant context set: {}", tenantId);
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
    private String extractTenantId(String uri) {
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
}
