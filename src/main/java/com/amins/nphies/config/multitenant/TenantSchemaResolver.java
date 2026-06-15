package com.amins.nphies.config.multitenant;

import com.amins.nphies.model.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the current Hibernate schema from TenantContext (set by JwtAuthFilter
 * for authenticated requests, or manually by auth controllers before JPA calls).
 * Falls back to the system schema for unauthenticated startup operations.
 */
@Component
public class TenantSchemaResolver implements CurrentTenantIdentifierResolver<String> {

    public static final String SYSTEM_SCHEMA = "nphies_system";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getOrNull();
        return tenantId != null ? "nphies_" + tenantId : SYSTEM_SCHEMA;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
