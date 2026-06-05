package com.bhagwat.scm.core.logging.config;

import com.bhagwat.scm.core.logging.provider.DynamicLogProvider;
import com.bhagwat.scm.core.logging.provider.impl.ApiHeaderLogProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);

    @ConditionalOnProperty(
            name = "service.core.log.method-entry-exit-enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    @Bean
    public DynamicLogProvider apiHeaderLogProvider(@Value("${service.core.filter.dynamic-logging.header-driver.header-key}")String headerKey){
        logger.info("initializing header driven dynamic log provider with header-key: {}", headerKey);
        return new ApiHeaderLogProvider(headerKey);
    }
}

