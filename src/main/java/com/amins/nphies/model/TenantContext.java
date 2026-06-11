package com.amins.nphies.model;

/**
 * ThreadLocal holder for the current tenant ID.
 * Set at request entry point (e.g. JWT filter or API gateway header).
 * Cleared after request completes to prevent memory leaks.
 *
 * Usage:
 *   TenantContext.set("tenant-abc-123");
 *   String tenantId = TenantContext.get(); // anywhere in the call stack
 *   TenantContext.clear();                 // always in finally block
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    public static void set(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID must not be blank");
        }
        TENANT_ID.set(tenantId);
    }

    public static String get() {
        String tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException(
                    "No tenant context set. Ensure TenantContext.set() is called " +
                            "at request entry (e.g. in TenantResolutionFilter).");
        }
        return tenantId;
    }

    public static String getOrNull() {
        return TENANT_ID.get();
    }

    /**
     * For tenant-scoped REST endpoints: returns the current tenant or fails
     * with 403 when the authenticated user has no tenant assigned.
     */
    public static String require() {
        String tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "No tenant assigned to the authenticated user");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    private TenantContext() {}
}
