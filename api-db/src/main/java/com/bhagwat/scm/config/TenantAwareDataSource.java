package com.bhagwat.scm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TenantAwareDataSource extends AbstractRoutingDataSource {

    private final com.bhagwat.scm.config.MultiTenancyProperties properties;
    private final DataSource targetDataSource;
    private final String baseSchema;

    // Pattern to extract schema name from JDBC URL
    private static final Pattern SCHEMA_PATTERN = Pattern.compile("jdbc:postgresql://[^/]+/([^?]+)");

    public TenantAwareDataSource(DataSource targetDataSource, com.bhagwat.scm.config.MultiTenancyProperties properties) {
        this.targetDataSource = targetDataSource;
        this.properties = properties;
        this.baseSchema = extractSchemaFromDataSource(targetDataSource);

        setTargetDataSources(java.util.Collections.singletonMap("default", targetDataSource));
        setDefaultTargetDataSource(targetDataSource);

        log.info("TenantAwareDataSource initialized with base schema: {}", baseSchema);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return com.bhagwat.scm.config.TenantContext.getTenantId();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = targetDataSource.getConnection();
        setSchemaForTenant(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = targetDataSource.getConnection(username, password);
        setSchemaForTenant(connection);
        return connection;
    }

    /**
     * Set the schema based on current tenant
     */
    private void setSchemaForTenant(Connection connection) throws SQLException {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            // No tenant context, fall back to public schema
            log.debug("No tenant context, using public schema");
            connection.setSchema("public");
            return;
        }

        // Use tenantId directly as the PostgreSQL schema name.
        // Each tenant maps to a schema with the same name (e.g. "public", "tenant1").
        // baseSchema is the database name (from the JDBC URL) — it is NOT a schema and
        // must not be appended, since schemas live *inside* the database.
        log.debug("Setting schema for tenant {}: {}", tenantId, tenantId);
        connection.setSchema(tenantId);
    }

    /**
     * Extract base schema name from DataSource JDBC URL
     */
    private String extractSchemaFromDataSource(DataSource dataSource) {
        try {
            Connection connection = dataSource.getConnection();
            String url = connection.getMetaData().getURL();
            connection.close();

            Matcher matcher = SCHEMA_PATTERN.matcher(url);
            if (matcher.find()) {
                String schema = matcher.group(1);
                log.info("Extracted base schema from JDBC URL: {}", schema);
                return schema;
            }

            log.warn("Could not extract schema from JDBC URL: {}", url);
            return "public"; // Default PostgreSQL schema

        } catch (SQLException e) {
            log.error("Error extracting schema from DataSource", e);
            return "public";
        }
    }
}