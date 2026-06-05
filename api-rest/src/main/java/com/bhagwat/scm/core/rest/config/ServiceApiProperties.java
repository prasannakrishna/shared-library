package com.bhagwat.scm.core.rest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds YAML config under "service.core.api-configs" to a map of named API definitions.
 *
 * Example YAML:
 * <pre>
 * service:
 *   core:
 *     api-configs:
 *       catalog-products:
 *         host: http://localhost:8089
 *         path: /api/catalog/products/all
 *         method: GET
 *       inventory-skus:
 *         host: http://localhost:8083
 *         path: /api/v1/skus
 *         method: GET
 * </pre>
 */
@ConfigurationProperties(prefix = "service.core")
@Data
public class ServiceApiProperties {

    private Map<String, ApiDefinition> apiConfigs = new HashMap<>();

    @Data
    public static class ApiDefinition {
        private String host;
        private String path;
        private HttpMethod method = HttpMethod.GET;
        private int responseTimeout;
        private int connectionTimeout;
        private int requestTimeout;
    }
}
