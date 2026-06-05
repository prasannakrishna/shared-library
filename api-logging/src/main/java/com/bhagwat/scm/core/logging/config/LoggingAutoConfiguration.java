package com.bhagwat.scm.core.logging.config;

import brave.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "service.core.logging.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LoggingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LoggingAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public Tracer tracer(Tracer tracer) {
        log.info(" Distributed tracing enabled via Spring Cloud Sleuth + Zipkin");
        return tracer;
    }
}
