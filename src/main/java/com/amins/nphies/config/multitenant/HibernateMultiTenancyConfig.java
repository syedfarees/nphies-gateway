package com.amins.nphies.config.multitenant;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Wires the Spring-managed multi-tenancy beans into Hibernate.
 * Class-name strings in application.yml cause Hibernate to call Class.newInstance()
 * (no-arg constructor), bypassing Spring DI. Passing bean instances here avoids that.
 */
@Configuration
@RequiredArgsConstructor
public class HibernateMultiTenancyConfig implements HibernatePropertiesCustomizer {

    private final SchemaTenantConnectionProvider connectionProvider;
    private final TenantSchemaResolver tenantResolver;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
    }
}
