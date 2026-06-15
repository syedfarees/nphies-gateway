package com.amins.nphies.config.multitenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Creates a new per-tenant MySQL schema and runs Flyway tenant migrations against it.
 * Also maintains the tenant_registry row in the system schema.
 *
 * Called by TenantRegistrationService on new tenant onboarding and by
 * BootstrapAdminInitializer at first-boot.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaProvisioner {

    private final DataSource dataSource;
    private final JdbcTemplate systemJdbcTemplate;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    /**
     * Provisions a brand-new tenant schema:
     * 1. CREATE SCHEMA IF NOT EXISTS
     * 2. Run Flyway tenant migrations
     * 3. Insert row in tenant_registry
     */
    public void provision(String tenantId, String displayName) {
        String schema = schemaName(tenantId);
        log.info("Provisioning tenant schema '{}'", schema);

        // 1. Create schema
        systemJdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS `" + schema + "`");

        // 2. Run Flyway tenant migrations against the new schema
        String tenantUrl = buildUrl(schema);
        Flyway flyway = Flyway.configure()
                .dataSource(tenantUrl, dbUsername, dbPassword)
                .schemas(schema)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(false)
                .load();
        flyway.migrate();

        // 3. Register in system schema
        systemJdbcTemplate.update(
                "INSERT INTO nphies_system.tenant_registry (tenant_id, schema_name, display_name, status) " +
                "VALUES (?, ?, ?, 'ACTIVE') " +
                "ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), status='ACTIVE'",
                tenantId, schema, displayName);

        log.info("Tenant '{}' provisioned — schema '{}'", tenantId, schema);
    }

    /** Returns true if the tenant is already registered in the system schema. */
    public boolean exists(String tenantId) {
        Integer count = systemJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nphies_system.tenant_registry WHERE tenant_id = ?",
                Integer.class, tenantId);
        return count != null && count > 0;
    }

    /** Returns true if there are no tenants registered at all (first boot). */
    public boolean isEmpty() {
        Integer count = systemJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nphies_system.tenant_registry",
                Integer.class);
        return count == null || count == 0;
    }

    public static String schemaName(String tenantId) {
        return "nphies_" + tenantId;
    }

    /** Builds a JDBC URL for a specific schema, reusing the base URL. */
    private String buildUrl(String schema) {
        // Strip trailing /? and any existing database name, then append schema
        String base = jdbcUrl.replaceAll("/[^/?]*(?=[?]|$)", "/");
        return base + schema + "?connectionTimeZone=UTC";
    }
}
