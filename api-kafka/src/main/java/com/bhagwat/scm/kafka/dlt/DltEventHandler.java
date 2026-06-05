package com.bhagwat.scm.kafka.dlt;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Hook interface for custom Dead Letter Topic (DLT) handling.
 *
 * Implement this bean in your service to add custom logic when an event
 * exhausts all retry attempts and lands on the DLT topic.
 *
 * <pre>{@code
 * @Component
 * public class MyDltHandler implements DltEventHandler {
 *     @Override
 *     public void handle(ConsumerRecord<?, ?> record, Exception cause) {
 *         // e.g. persist to DB, send alert, notify support team
 *     }
 * }
 * }</pre>
 *
 * If no custom implementation is provided, {@link DefaultDltEventHandler} is used.
 */
public interface DltEventHandler {

    /**
     * Called when a message has been routed to the DLT after exhausting all retries.
     *
     * @param record the original consumer record that failed
     * @param cause  the last exception that triggered the DLT routing
     */
    void handle(ConsumerRecord<?, ?> record, Exception cause);
}
