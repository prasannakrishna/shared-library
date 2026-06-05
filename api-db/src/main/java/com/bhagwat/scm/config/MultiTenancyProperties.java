package com.bhagwat.scm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "multitenancy")
public class MultiTenancyProperties {

    /** Claim name for tenant ID in JWT (default: tenantId) */
    private String tenantIdClaimName = "tenantId";

    /** Enable multi-tenancy (default: true) */
    private boolean enabled = true;

    /** @deprecated No longer used — tenantId is used directly as the schema name */
    @Deprecated
    private String schemaSeparator = "_";

    /** Default tenant ID to use when no tenant is found in JWT */
    private String defaultTenantId;

    /**
     * If true and no tenant found, throws exception.
     * If false, falls back to defaultTenantId or continues without tenant.
     */
    private boolean failOnMissingTenant = true;

    /** Tenant schema provisioning configuration */
    private Provisioning provisioning = new Provisioning();

    @Data
    public static class Provisioning {
        /**
         * Classpath location of Flyway tenant migration scripts.
         * These are run in each new tenant's schema when it is provisioned.
         */
        private String migrationsLocation = "classpath:db/migration/tenants";

        /**
         * Service name stored in tenant_registry to distinguish per-service provisioning.
         * Defaults to ${spring.application.name} if left blank.
         */
        private String serviceName;
    }
}