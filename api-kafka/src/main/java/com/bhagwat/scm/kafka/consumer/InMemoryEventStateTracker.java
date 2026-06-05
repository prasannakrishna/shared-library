package com.bhagwat.scm.kafka.consumer;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of {@link EventStateTracker}.
 * Suitable for single-instance services or testing.
 *
 * For distributed environments (multiple replicas), replace with a
 * Redis-backed or database-backed implementation.
 */
@Slf4j
public class InMemoryEventStateTracker implements EventStateTracker {

    private final ConcurrentHashMap<String, EventProcessingState> stateMap = new ConcurrentHashMap<>();

    @Override
    public void setState(String eventId, EventProcessingState state) {
        log.debug("EventStateTracker: eventId={} -> {}", eventId, state);
        stateMap.put(eventId, state);
    }

    @Override
    public Optional<EventProcessingState> getState(String eventId) {
        return Optional.ofNullable(stateMap.get(eventId));
    }

    @Override
    public boolean isAlreadyProcessed(String eventId) {
        return EventProcessingState.COMPLETED.equals(stateMap.get(eventId));
    }

    @Override
    public void remove(String eventId) {
        stateMap.remove(eventId);
    }
}
