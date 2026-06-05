package com.bhagwat.scm.kafka.consumer;

import java.util.Optional;

/**
 * Tracks the processing state of Kafka events by their eventId.
 *
 * The default implementation is in-memory ({@link InMemoryEventStateTracker}).
 * Override this bean in your application to back the state with Redis or a database
 * for distributed idempotency guarantees.
 *
 * <pre>{@code
 * @Bean
 * @Primary
 * public EventStateTracker redisEventStateTracker(RedisClient redisClient) {
 *     return new RedisEventStateTracker(redisClient);
 * }
 * }</pre>
 */
public interface EventStateTracker {

    /** Record or update the state for the given eventId. */
    void setState(String eventId, EventProcessingState state);

    /** Get the current state for the given eventId if it exists. */
    Optional<EventProcessingState> getState(String eventId);

    /**
     * Returns true if the event has already been successfully processed
     * (state == COMPLETED). Use this to implement idempotent consumers.
     */
    boolean isAlreadyProcessed(String eventId);

    /** Remove the tracking entry once you no longer need it. */
    void remove(String eventId);
}
