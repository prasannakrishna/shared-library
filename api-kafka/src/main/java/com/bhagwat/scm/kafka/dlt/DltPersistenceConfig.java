package com.bhagwat.scm.kafka.dlt;

import org.springframework.context.annotation.Configuration;

/**
 * Marker config for DLT persistence.
 * The FailedEventLog entity will be picked up by the host service's default entity scan
 * as long as the service scans "com.bhagwat.scm" (which all our services do).
 * No @EntityScan or @EnableJpaRepositories here to avoid interfering with host services.
 */
@Configuration
public class DltPersistenceConfig {
}
