package com.bhagwat.scm.observability.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Injects observability property defaults at the lowest priority so that values
 * in the consuming service's {@code application.properties} always override them.
 *
 * <p>Derived properties (e.g. {@code management.otlp.tracing.endpoint}) are built
 * from the {@code observability.otel.endpoint} value, which the consuming service
 * can set in one place to redirect all telemetry to a different collector.
 *
 * <p>Registered in {@code META-INF/spring.factories}.
 */
public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE_NAME = "observabilityDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {

        MutablePropertySources sources = environment.getPropertySources();

        // Don't add twice (can happen in tests)
        if (sources.contains(SOURCE_NAME)) return;

        // Read the high-level observability.* props (may already be set by app)
        String otelEndpoint  = environment.getProperty("observability.otel.endpoint",   "http://localhost:4318");
        String sampleRate    = environment.getProperty("observability.tracing.sample-rate", "1.0");
        String appName       = environment.getProperty("spring.application.name", "app");
        String appVersion    = environment.getProperty("spring.application.version", "unknown");

        Map<String, Object> defaults = new LinkedHashMap<>();

        // ── Actuator exposure ──────────────────────────────────────────────
        defaults.put("management.endpoints.web.exposure.include",
                "health,info,metrics,prometheus");
        defaults.put("management.endpoint.health.show-details",     "always");
        defaults.put("management.endpoint.health.probes.enabled",   "true");
        defaults.put("management.info.env.enabled",                 "true");

        // ── Tracing ────────────────────────────────────────────────────────
        defaults.put("management.tracing.sampling.probability",     sampleRate);
        defaults.put("management.otlp.tracing.endpoint",            otelEndpoint + "/v1/traces");
        defaults.put("management.otlp.tracing.timeout",             "10s");

        // ── Metrics ────────────────────────────────────────────────────────
        defaults.put("management.otlp.metrics.export.url",          otelEndpoint + "/v1/metrics");
        defaults.put("management.otlp.metrics.export.step",         "30s");
        defaults.put("management.otlp.metrics.export.resource-attributes.service.name", appName);

        // ── OTel resource attributes (visible in Tempo span details) ───────
        defaults.put("otel.resource.attributes.service.name",       appName);
        defaults.put("otel.resource.attributes.service.version",    appVersion);
        defaults.put("otel.resource.attributes.deployment.environment",
                environment.getProperty("observability.environment", "local"));

        // ── Logging pattern (traceId + spanId in every log line) ──────────
        defaults.put("logging.pattern.level",
                "%5p [" + appName + ",%X{traceId:-},%X{spanId:-}]");

        // addLast = lowest priority → consuming service's application.properties wins
        sources.addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        // Run after standard config data post-processors so we can read app properties
        return Ordered.LOWEST_PRECEDENCE - 5;
    }
}
