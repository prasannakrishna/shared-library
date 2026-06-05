package com.bhagwat.scm.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        log.debug("Setting tenant ID: {}", tenantId);
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        String tenantId = currentTenant.get();
        log.debug("Getting tenant ID: {}", tenantId);
        return tenantId;
    }

    public static void clear() {
        log.debug("Clearing tenant context");
        currentTenant.remove();
    }

    public static boolean hasTenant() {
        return currentTenant.get() != null;
    }
}