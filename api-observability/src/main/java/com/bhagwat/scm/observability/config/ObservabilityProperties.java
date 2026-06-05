package com.bhagwat.scm.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Observability configuration properties.  All values have localhost defaults so
 * the service starts with zero extra config in local development.
 *
 * Prefix: {@code observability}
 */
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /** Whether the observability stack is active (default: true). */
    private boolean enabled = true;

    /** Deployment environment label attached to every span, metric and log stream. */
    private String environment = "local";

    private final Otel otel = new Otel();
    private final Loki loki = new Loki();
    private final Tracing tracing = new Tracing();

    // ── OTel Collector ───────────────────────────────────────────────────────
    public static class Otel {
        /**
         * Base URL of the OpenTelemetry Collector OTLP HTTP receiver.
         * Traces go to {@code /v1/traces}, metrics to {@code /v1/metrics}.
         */
        private String endpoint = "http://localhost:4318";

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    // ── Loki ─────────────────────────────────────────────────────────────────
    public static class Loki {
        /** Base URL of the Loki HTTP push API ({@code /loki/api/v1/push} is appended). */
        private String url = "http://localhost:3100";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    // ── Tracing ───────────────────────────────────────────────────────────────
    public static class Tracing {
        /**
         * Fraction of requests to sample (1.0 = 100 %, 0.1 = 10 %).
         * Lower this in high-traffic production environments.
         */
        private double sampleRate = 1.0;

        public double getSampleRate() { return sampleRate; }
        public void setSampleRate(double sampleRate) { this.sampleRate = sampleRate; }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public Otel getOtel() { return otel; }
    public Loki getLoki() { return loki; }
    public Tracing getTracing() { return tracing; }
}
