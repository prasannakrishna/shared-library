package com.bhagwat.scm.observability.config;

import com.bhagwat.scm.observability.filter.ObservabilityRequestLoggingFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Auto-configuration activated by {@link com.bhagwat.scm.observability.annotation.EnableObservability}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Tags every Micrometer metric with {@code service_name} and {@code environment}
 *       so Grafana dashboards can filter by service without changing PromQL queries.</li>
 *   <li>Logs a startup banner summarising where telemetry is going.</li>
 * </ul>
 *
 * <p>The heavy lifting (OTLP trace/metric export, sampling probability) is wired via
 * Spring Boot properties that {@link ObservabilityEnvironmentPostProcessor} injects as
 * low-priority defaults, so {@code application.properties} values always win.
 */
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(name = "observability.enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityAutoConfiguration.class);

    /**
     * Tags every Micrometer metric with {@code environment} so Grafana queries can
     * filter by environment without changing PromQL.
     *
     * <p>NOTE: {@code service_name} is intentionally NOT added here.
     * The OTLP resource attribute {@code service.name} (set via
     * {@code otel.resource.attributes.service.name}) is already converted to the
     * {@code job} Prometheus label by the OTel Collector's
     * {@code resource_to_telemetry_conversion}. Adding it again as a Micrometer common
     * tag would cause Prometheus to receive two values and concatenate them as
     * {@code sellerService;sellerService}.
     * Use {@code job="<serviceName>"} in all Grafana/PromQL queries.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> observabilityMetricsCustomizer(
            ObservabilityProperties props,
            Environment env) {

        String serviceName = env.getProperty("spring.application.name", "unknown");

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  SCM Observability  →  service: {}  env: {}", serviceName, props.getEnvironment());
        log.info("  Traces   →  {}/v1/traces", props.getOtel().getEndpoint());
        log.info("  Metrics  →  {}/v1/metrics", props.getOtel().getEndpoint());
        log.info("  Logs     →  {}/loki/api/v1/push", props.getLoki().getUrl());
        log.info("  Sampling →  {}%", (int) (props.getTracing().getSampleRate() * 100));
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return registry -> registry.config()
                .commonTags("environment", props.getEnvironment());
    }

    /**
     * Logs every HTTP request at INFO level after it completes, including the
     * {@code traceId} and {@code spanId} from MDC.  This makes traces visible in
     * Loki even when the service itself has no explicit log statements in its
     * controllers or service layer.
     *
     * <p>Disable with {@code observability.request-logging.enabled=false}.
     */
    /**
     * Wraps the filter in a {@link FilterRegistrationBean} so that Spring AOP never
     * CGLIB-proxies the filter instance itself.  Direct {@code @Bean} filter registration
     * causes CGLIB (via api-logging's AOP) to subclass {@code OncePerRequestFilter},
     * which skips the constructor via Objenesis and leaves the internal {@code logger}
     * field null — crashing on startup.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(OncePerRequestFilter.class)
    @ConditionalOnProperty(
            name = "observability.request-logging.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public FilterRegistrationBean<ObservabilityRequestLoggingFilter> observabilityRequestLoggingFilter() {
        FilterRegistrationBean<ObservabilityRequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ObservabilityRequestLoggingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        registration.setName("observabilityRequestLoggingFilter");
        return registration;
    }
}
