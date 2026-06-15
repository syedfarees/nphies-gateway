package com.amins.nphies.config.multitenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Switches MySQL schema per Hibernate transaction by executing USE `schema_name`.
 * The single underlying DataSource (HikariCP) connects to the MySQL server without
 * a default database — the schema is always set explicitly on connection checkout.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String schema) throws SQLException {
        Connection conn = dataSource.getConnection();
        try {
            conn.createStatement().execute("USE `" + sanitize(schema) + "`");
        } catch (SQLException ex) {
            log.error("Failed to switch to schema '{}': {}", schema, ex.getMessage());
            conn.close();
            throw ex;
        }
        return conn;
    }

    @Override
    public void releaseConnection(String schema, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new UnknownUnwrapTypeException(unwrapType);
    }

    /** Rejects schema names that contain anything other than letters, digits, underscores, and hyphens. */
    private static String sanitize(String schema) {
        if (!schema.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }
        return schema;
    }
}
