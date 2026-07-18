package com.nexus.common.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * A DataSource wrapper that automatically executes
 * {@code SET LOCAL app.tenant_id = '...'} when a transaction begins.
 *
 * <p><b>How it works:</b></p>
 * <ol>
 *   <li>Spring's {@code @Transactional} gets a Connection from the pool</li>
 *   <li>Spring calls {@code setAutoCommit(false)} to start the transaction</li>
 *   <li>This wrapper intercepts that call and runs {@code SET LOCAL} immediately after</li>
 *   <li>{@code SET LOCAL} is now inside the transaction — Postgres resets it on commit/rollback</li>
 *   <li>No tenant context leaks to the next request through a pooled connection</li>
 * </ol>
 *
 * <p>If {@link TenantContext} has no tenant ID (e.g., a non-tenant-scoped
 * endpoint), no {@code SET LOCAL} is executed and RLS fail-closes (zero rows).
 *
 * @see TenantContext
 * @see TenantContextFilter
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrapConnection(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrapConnection(super.getConnection(username, password));
    }

    /**
     * Wraps the real JDBC Connection in a proxy that intercepts
     * {@code setAutoCommit(false)} — the signal that a transaction has started.
     */
    private Connection wrapConnection(Connection realConnection) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new TenantConnectionHandler(realConnection)
        );
    }

    /**
     * Invocation handler that delegates all Connection methods to the
     * real connection, but intercepts {@code setAutoCommit(false)} to
     * inject the RLS tenant context.
     */
    private static class TenantConnectionHandler implements InvocationHandler {

        private final Connection realConnection;
        private boolean tenantSet = false;

        TenantConnectionHandler(Connection realConnection) {
            this.realConnection = realConnection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Intercept setAutoCommit(false) — this is when Spring starts a transaction
            if ("setAutoCommit".equals(method.getName())
                    && args != null && args.length == 1
                    && Boolean.FALSE.equals(args[0])) {
                Object result = method.invoke(realConnection, args);
                if (!tenantSet) {
                    applyTenantContext();
                }
                return result;
            }

            // For unwrap() and isWrapperFor(), delegate to real connection
            if ("unwrap".equals(method.getName())) {
                Class<?> targetClass = (Class<?>) args[0];
                if (targetClass.isInstance(realConnection)) {
                    return realConnection;
                }
                return method.invoke(realConnection, args);
            }
            if ("isWrapperFor".equals(method.getName())) {
                Class<?> targetClass = (Class<?>) args[0];
                if (targetClass.isInstance(realConnection)) {
                    return true;
                }
                return method.invoke(realConnection, args);
            }

            // All other methods — delegate directly
            try {
                return method.invoke(realConnection, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }

        /**
         * Executes {@code SET LOCAL app.tenant_id = '...'} on the connection.
         * Only called once per connection checkout (the flag prevents duplicates).
         *
         * <p>The tenant ID is validated as a UUID before interpolation
         * to prevent SQL injection.
         */
        private void applyTenantContext() throws SQLException {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                // Validate UUID format — defense against SQL injection
                try {
                    UUID.fromString(tenantId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tenant ID in context, skipping SET LOCAL: {}", tenantId);
                    return;
                }

                try (Statement stmt = realConnection.createStatement()) {
                    stmt.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");
                }
                tenantSet = true;
                log.debug("SET LOCAL app.tenant_id = '{}' on connection", tenantId);
            }
        }
    }
}
