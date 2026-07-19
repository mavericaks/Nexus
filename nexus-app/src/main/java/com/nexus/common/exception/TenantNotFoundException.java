package com.nexus.common.exception;

import java.util.UUID;

/**
 * Thrown when a tenant ID in the URL doesn't match any tenant in the database.
 * Maps to HTTP 404.
 */
public class TenantNotFoundException extends RuntimeException {

    private final UUID tenantId;

    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found: " + tenantId);
        this.tenantId = tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
