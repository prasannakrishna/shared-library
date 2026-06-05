package com.bhagwat.scm.kafka.envelope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Standard wrapper for all Kafka events published through this library.
 * Carries the business payload along with metadata needed for tracing,
 * idempotency checking, and retry visibility.
 *
 * @param <T> the business payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEventEnvelope<T> {

    /** Unique ID for this event — used for idempotency checks. */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /** Logical event type name (e.g. "OrderCreated", "PaymentFailed"). */
    private String eventType;

    /** Correlation / trace ID propagated from upstream. */
    private String correlationId;

    /** Source service that published this event. */
    private String source;

    /** UTC timestamp of event creation. */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /** The business payload. */
    private T payload;

    /** How many times this event has been retried (0 = first attempt). */
    @Builder.Default
    private int retryCount = 0;

    /** Arbitrary key-value headers for extensibility. */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
}
