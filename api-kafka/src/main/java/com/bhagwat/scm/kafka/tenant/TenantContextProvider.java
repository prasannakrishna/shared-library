package com.bhagwat.scm.kafka.tenant;

/**
 * Bridge interface between api-kafka and the application's tenant context.
 *
 * api-kafka has no dependency on api-db or any specific TenantContext implementation.
 * Instead, the consuming service provides one bean that implements this interface
 * and delegates to whatever tenant mechanism they use (ThreadLocal, MDC, etc.).
 *
 * The library uses this interface to:
 *   1. PRODUCER side — automatically stamp "X-Tenant-Id" as a Kafka header on every outgoing message
 *   2. CONSUMER side — automatically restore the tenant context before calling process(),
 *                      and clear it in the finally block after the call completes
 *
 * Default bean: {@link NoOpTenantContextProvider} (no-op — does nothing if not overridden).
 *
 * How to wire it up in any service that uses api-db:
 * <pre>{@code
 * @Configuration
 * public class KafkaTenantConfig {
 *
 *     @Bean
 *     @Primary
 *     public TenantContextProvider tenantContextProvider() {
 *         return new TenantContextProvider() {
 *             @Override public String getTenantId() { return TenantContext.getTenantId(); }
 *             @Override public void setTenantId(String id) { TenantContext.setTenantId(id); }
 *             @Override public void clear() { TenantContext.clear(); }
 *         };
 *     }
 * }
 * }</pre>
 */
public interface TenantContextProvider {

    /** Header name used to carry tenant ID in Kafka messages. */
    String TENANT_HEADER = "X-Tenant-Id";

    /**
     * Return the tenant ID from the current execution context (e.g. ThreadLocal).
     * Return null if no tenant is set.
     */
    String getTenantId();

    /**
     * Set the tenant ID into the current execution context.
     * Called by the consumer before invoking business logic.
     */
    void setTenantId(String tenantId);

    /**
     * Clear the tenant ID from the current execution context.
     * Always called in a finally block after consumer processing completes.
     */
    void clear();
}
