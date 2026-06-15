package com.amins.nphies.config.multitenant;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Bootstraps the system schema (nphies_system) at application startup:
 * 1. Creates nphies_system if it doesn't exist
 * 2. Runs system Flyway migrations (tenant_registry table)
 * 3. Exposes a JdbcTemplate scoped to nphies_system for registry operations
 */
@Configuration
@Slf4j
public class SystemSchemaInitializer {

    private static final String SYSTEM_SCHEMA = "nphies_system";

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean(name = "systemDataSource")
    public DataSource systemDataSource() {
        String systemUrl = buildSystemUrl();

        // Ensure schema exists before Flyway tries to connect to it
        try {
            DriverManagerDataSource noSchemaDs = new DriverManagerDataSource();
            noSchemaDs.setUrl(buildNoSchemaUrl());
            noSchemaDs.setUsername(username);
            noSchemaDs.setPassword(password);
            new JdbcTemplate(noSchemaDs).execute(
                    "CREATE SCHEMA IF NOT EXISTS `" + SYSTEM_SCHEMA + "`");
        } catch (Exception e) {
            log.warn("Could not pre-create system schema (may already exist): {}", e.getMessage());
        }

        // Run Flyway system migrations
        Flyway flyway = Flyway.configure()
                .dataSource(systemUrl, username, password)
                .schemas(SYSTEM_SCHEMA)
                .locations("classpath:db/migration/system")
                .baselineOnMigrate(false)
                .load();
        flyway.migrate();
        log.info("System schema '{}' initialized", SYSTEM_SCHEMA);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(systemUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    @Bean(name = "systemJdbcTemplate")
    public JdbcTemplate systemJdbcTemplate(DataSource systemDataSource) {
        return new JdbcTemplate(systemDataSource);
    }

    private String buildSystemUrl() {
        return buildNoSchemaUrl() + SYSTEM_SCHEMA + "?connectionTimeZone=UTC";
    }

    private String buildNoSchemaUrl() {
        // Remove everything from the last '/' before '?' or end of string
        return jdbcUrl.replaceAll("/[^/?]*(?=[?]|$)", "/");
    }
}
