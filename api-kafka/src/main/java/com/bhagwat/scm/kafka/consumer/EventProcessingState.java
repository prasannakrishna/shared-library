package com.bhagwat.scm.kafka.consumer;

/**
 * Lifecycle states of a Kafka event being processed by a consumer.
 */
public enum EventProcessingState {

    /** Event has been received from Kafka but not yet handed to business logic. */
    RECEIVED,

    /** Business logic is currently executing. */
    PROCESSING,

    /** Business logic completed successfully. */
    COMPLETED,

    /** Business logic threw an exception; will be retried if attempts remain. */
    FAILED,

    /** All retry attempts exhausted; event has been published to the DLT topic. */
    DEAD_LETTERED
}
