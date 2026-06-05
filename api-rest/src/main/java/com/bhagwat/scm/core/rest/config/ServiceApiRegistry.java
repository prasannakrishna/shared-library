package com.bhagwat.scm.core.rest.config;

import com.bhagwat.scm.core.rest.api.ApiConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of pre-built ApiConfig instances from YAML.
 * Inject this + ApiClient in any service to make inter-service calls.
 *
 * Usage:
 *   apiClient.invoke(registry.getConfig("catalog-products"), Map.class);
 */
@RequiredArgsConstructor
@Slf4j
public class ServiceApiRegistry {

    private final ServiceApiProperties properties;
    private final Map<String, ApiConfig> configs = new HashMap<>();

    @PostConstruct
    void init() {
        properties.getApiConfigs().forEach((name, def) -> {
            ApiConfig cfg = new ApiConfig();
            cfg.setHost(def.getHost());
            cfg.setApiPath(def.getPath());
            cfg.setHttpMethod(def.getMethod());
            if (def.getResponseTimeout() > 0 || def.getConnectionTimeout() > 0 || def.getRequestTimeout() > 0) {
                cfg.setOverrideDefaultHttpProperties(true);
                cfg.setResponseTimeout(def.getResponseTimeout());
                cfg.setConnectionTimeout(def.getConnectionTimeout());
                cfg.setRequestTimeout(def.getRequestTimeout());
            }
            configs.put(name, cfg);
        });
        log.info("ServiceApiRegistry initialized with {} configs: {}", configs.size(), configs.keySet());
    }

    public ApiConfig getConfig(String name) {
        ApiConfig cfg = configs.get(name);
        if (cfg == null) {
            throw new IllegalArgumentException("No API config registered for: " + name);
        }
        return cfg;
    }

    /**
     * Get config with path variable substitution.
     * E.g., config path = "/api/v1/skus/{id}" → resolves to "/api/v1/skus/123"
     */
    public ApiConfig getConfig(String name, String... pathSegments) {
        ApiConfig base = getConfig(name);
        if (pathSegments.length == 0) return base;
        ApiConfig copy = new ApiConfig();
        copy.setHost(base.getHost());
        copy.setHttpMethod(base.getHttpMethod());
        copy.setOverrideDefaultHttpProperties(base.isOverrideDefaultHttpProperties());
        copy.setResponseTimeout(base.getResponseTimeout());
        copy.setConnectionTimeout(base.getConnectionTimeout());
        copy.setRequestTimeout(base.getRequestTimeout());
        String path = base.getApiPath();
        for (String seg : pathSegments) {
            path = path.replaceFirst("\\{[^}]+}", seg);
        }
        copy.setApiPath(path);
        return copy;
    }
}
