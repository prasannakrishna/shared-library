package com.bhagwat.scm.kafka.annotation;

import com.bhagwat.scm.kafka.config.KafkaAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation to your Spring Boot main class to enable
 * the Kafka messaging library.
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableKafkaMessaging
 * public class MyApplication { ... }
 * }</pre>
 *
 * Configure via application.properties using the {@code api.kafka.*} prefix.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(KafkaAutoConfiguration.class)
public @interface EnableKafkaMessaging {
}
