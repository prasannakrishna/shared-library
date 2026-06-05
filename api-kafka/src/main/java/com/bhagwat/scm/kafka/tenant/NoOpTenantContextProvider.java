package com.bhagwat.scm.kafka.tenant;

/**
 * Default no-op implementation of {@link TenantContextProvider}.
 * Used when the service has not configured multi-tenancy.
 * Produces no tenant headers and restores no tenant context.
 */
public class NoOpTenantContextProvider implements TenantContextProvider {

    @Override
    public String getTenantId() {
        return null;
    }

    @Override
    public void setTenantId(String tenantId) {
        // no-op
    }

    @Override
    public void clear() {
        // no-op
    }
}
