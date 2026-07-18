package com.nexus.common.multitenancy;

/**
 * Thread-local holder for the current tenant ID.
 *
 * <p>Set by {@link TenantContextFilter} at the start of each HTTP request,
 * read by {@link TenantAwareDataSource} when a database connection is
 * checked out. Cleared automatically after the request completes.
 *
 * <p>ThreadLocal is safe here because Tomcat assigns one thread per request.
 * The filter's {@code finally} block calls {@link #clear()} to prevent
 * tenant leakage between requests on the same thread.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class — no instances
    }

    /** Set the tenant ID for the current request thread. */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /** Get the tenant ID for the current request thread, or null if not set. */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Remove the tenant ID from the current thread.
     * MUST be called in a finally block to prevent tenant leakage
     * when the thread is returned to Tomcat's thread pool.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
