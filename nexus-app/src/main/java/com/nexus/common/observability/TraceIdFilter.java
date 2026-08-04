package com.nexus.common.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that generates a unique trace ID for every HTTP request
 * and injects it into Logback's MDC (Mapped Diagnostic Context).
 *
 * <p>With this filter, <b>every</b> log line emitted during a request
 * automatically includes a {@code traceId} field. This makes it trivial
 * to correlate all log lines for a single API call — just grep for the
 * trace ID.
 *
 * <p>The trace ID is also returned in the {@code X-Trace-Id} response
 * header. API consumers can include this value in bug reports or
 * support tickets so engineers can find the exact logs.
 *
 * <p>Runs at {@code @Order(-1)} — before the TenantContextFilter (Order 0)
 * and well after the Spring Security filter chain (Order -100). This
 * ensures the trace ID is available for all downstream logging.
 */
@Component
@Order(-1)
public class TraceIdFilter implements Filter {

    /** MDC key for the request trace ID. */
    public static final String TRACE_ID_KEY = "traceId";

    /** HTTP response header exposing the trace ID to API consumers. */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_KEY, traceId);
        try {
            if (response instanceof HttpServletResponse httpResponse) {
                httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
