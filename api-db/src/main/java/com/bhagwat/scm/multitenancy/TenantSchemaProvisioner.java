package com.bhagwat.scm.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Provisions a PostgreSQL schema for a new tenant.
 *
 * Responsibilities:
 *   1. CREATE SCHEMA IF NOT EXISTS {tenantId}
 *   2. Run Flyway migrations from classpath:db/migration/tenants into that schema
 *   3. Track provisioning state in public.tenant_registry (idempotent)
 *
 * Uses the raw (pre-routing) DataSource so it always operates in the public
 * schema for registry reads/writes, and targets specific tenant schemas for DDL.
 */
@Slf4j
public class TenantSchemaProvisioner {

    private static final String REGISTRY_DDL = """
            CREATE TABLE IF NOT EXISTS public.tenant_registry (
                tenant_id       VARCHAR(100)  NOT NULL,
                service_name    VARCHAR(100)  NOT NULL,
                status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                provisioned_at  TIMESTAMPTZ,
                error_message   TEXT,
                retry_count     INT           NOT NULL DEFAULT 0,
                CONSTRAINT pk_tenant_registry  PRIMARY KEY (tenant_id, service_name)
            )
            """;

    private final DataSource rawDataSource;
    private final JdbcTemplate jdbc;
    private final String serviceName;
    private final String migrationsLocation;

    public TenantSchemaProvisioner(DataSource rawDataSource,
                                   String serviceName,
                                   String migrationsLocation) {
        this.rawDataSource     = rawDataSource;
        this.jdbc              = new JdbcTemplate(rawDataSource);
        this.serviceName       = serviceName;
        this.migrationsLocation = migrationsLocation;
        ensureRegistryTable();
    }

    /**
     * Idempotent — safe to call multiple times for the same tenant.
     * If already PROVISIONED, returns immediately without any DB work.
     */
    public void provision(String tenantId) {
        String schema = sanitize(tenantId);

        if (isAlreadyProvisioned(schema)) {
            log.info("Tenant '{}' already provisioned for '{}', skipping", schema, serviceName);
            return;
        }

        upsertStatus(schema, "PENDING", null);
        log.info("Provisioning tenant '{}' for service '{}'", schema, serviceName);

        try {
            createSchemaIfNotExists(schema);
            runFlywayMigrations(schema);
            upsertStatus(schema, "PROVISIONED", null);
            log.info("Tenant '{}' provisioned successfully for '{}'", schema, serviceName);
        } catch (Exception e) {
            upsertStatus(schema, "FAILED", truncate(e.getMessage(), 500));
            throw new TenantProvisioningException(
                    "Failed to provision tenant '" + schema + "' for service '" + serviceName + "'", e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void createSchemaIfNotExists(String schema) {
        try (Connection conn = rawDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Schema name is sanitized — alphanumeric + underscore only
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");
            log.info("Schema '{}' created (or already existed)", schema);
        } catch (Exception e) {
            throw new TenantProvisioningException("Failed to create schema: " + schema, e);
        }
    }

    private void runFlywayMigrations(String schema) {
        Flyway flyway = Flyway.configure()
                .dataSource(rawDataSource)
                .schemas(schema)
                .locations(migrationsLocation)
                .table("flyway_schema_history")
                // For the default (public) schema, tables may already exist from
                // Hibernate ddl-auto=update — baseline marks current state without
                // re-running migrations. New tenant schemas run all migrations.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        int applied = flyway.migrate().migrationsExecuted;
        log.info("Flyway applied {} migration(s) to schema '{}'", applied, schema);
    }

    private boolean isAlreadyProvisioned(String schema) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.tenant_registry "
                + "WHERE tenant_id = ? AND service_name = ? AND status = 'PROVISIONED'",
                Integer.class, schema, serviceName);
        return count != null && count > 0;
    }

    private void upsertStatus(String schema, String status, String errorMessage) {
        Timestamp provisionedAt = "PROVISIONED".equals(status)
                ? Timestamp.from(Instant.now()) : null;

        jdbc.update("""
                INSERT INTO public.tenant_registry
                    (tenant_id, service_name, status, provisioned_at, error_message)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, service_name) DO UPDATE SET
                    status         = EXCLUDED.status,
                    provisioned_at = EXCLUDED.provisioned_at,
                    error_message  = EXCLUDED.error_message,
                    retry_count    = public.tenant_registry.retry_count + 1
                """,
                schema, serviceName, status, provisionedAt, errorMessage);
    }

    private void ensureRegistryTable() {
        try {
            jdbc.execute(REGISTRY_DDL);
            log.debug("public.tenant_registry table ensured");
        } catch (Exception e) {
            throw new TenantProvisioningException("Failed to initialize tenant_registry table", e);
        }
    }

    /** Only allow alphanumeric + underscore to prevent schema injection. */
    private String sanitize(String tenantId) {
        if (tenantId == null || !tenantId.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid tenantId '" + tenantId
                    + "': only alphanumeric characters and underscores are allowed");
        }
        return tenantId.toLowerCase();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
