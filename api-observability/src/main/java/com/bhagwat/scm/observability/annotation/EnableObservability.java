package com.bhagwat.scm.observability.annotation;

import com.bhagwat.scm.observability.config.ObservabilityAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables the SCM observability stack (traces → Tempo, metrics → Prometheus, logs → Loki)
 * via OpenTelemetry Collector.
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableObservability
 * public class OrderServiceApplication { }
 * }</pre>
 *
 * <b>Minimal application.properties:</b>
 * <pre>
 * spring.application.name=orderService
 * logging.config=classpath:logback-observability.xml
 * </pre>
 *
 * <b>Optional overrides (all have sensible localhost defaults):</b>
 * <pre>
 * observability.otel.endpoint=http://otel-collector:4318
 * observability.loki.url=http://loki:3100
 * observability.tracing.sample-rate=0.1
 * observability.environment=production
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ObservabilityAutoConfiguration.class)
public @interface EnableObservability {
}
