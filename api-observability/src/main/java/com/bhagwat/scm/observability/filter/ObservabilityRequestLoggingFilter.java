package com.bhagwat.scm.observability.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every HTTP request at INFO level after it completes.
 *
 * <p>Runs AFTER {@code ServerHttpObservationFilter} (which starts the OTel span and
 * populates MDC with {@code traceId} / {@code spanId}), so every log line this filter
 * emits appears in Loki with a non-empty traceId that links directly to the trace
 * in Tempo.
 *
 * <p>Format:
 * <pre>
 *   HTTP GET /api/v1/seller/suppliers → 200 (34 ms) traceId=4bf92f3577b34da6...
 * </pre>
 *
 * Disable with {@code observability.request-logging.enabled=false}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class ObservabilityRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long ms = System.currentTimeMillis() - start;
            String traceId = MDC.get("traceId");
            String spanId  = MDC.get("spanId");

            log.info("HTTP {} {} → {} ({} ms)  traceId={} spanId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    ms,
                    traceId != null ? traceId : "-",
                    spanId  != null ? spanId  : "-");
        }
    }

    /** Skip actuator health/info probes — they're noise. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/health")
            || uri.startsWith("/actuator/info")
            || uri.startsWith("/actuator/prometheus");
    }
}
