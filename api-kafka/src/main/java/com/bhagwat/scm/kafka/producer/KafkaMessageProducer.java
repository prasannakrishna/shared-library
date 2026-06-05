package com.bhagwat.scm.kafka.producer;

import com.bhagwat.scm.kafka.envelope.KafkaEventEnvelope;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Primary interface for publishing messages to Kafka topics.
 *
 * All {@code send*} methods are synchronous and block until the broker
 * acknowledges the message (based on the {@code acks} producer config).
 * Use the {@code sendAsync*} variants for fire-and-forget / non-blocking flows.
 */
public interface KafkaMessageProducer {

    // ── Simple sends ──────────────────────────────────────────────────────────

    /** Publish {@code payload} serialised as JSON to {@code topic}. No partition key. */
    <T> void send(String topic, T payload);

    /** Publish {@code payload} to {@code topic} with an explicit partition {@code key}. */
    <T> void send(String topic, String key, T payload);

    // ── Envelope sends ────────────────────────────────────────────────────────

    /**
     * Publish a pre-built {@link KafkaEventEnvelope} (retains eventId, correlationId, headers, etc.).
     * Use this when you need full control over the event metadata.
     */
    <T> void sendEnvelope(String topic, KafkaEventEnvelope<T> envelope);

    /**
     * Same as {@link #sendEnvelope(String, KafkaEventEnvelope)} but uses
     * {@code envelope.getEventId()} as the partition key for ordering.
     */
    <T> void sendEnvelopeKeyed(String topic, KafkaEventEnvelope<T> envelope);

    // ── Raw ProducerRecord ────────────────────────────────────────────────────

    /**
     * Low-level send using a fully constructed {@link ProducerRecord}.
     * Use when you need to set custom Kafka headers or a specific partition.
     */
    void sendRecord(ProducerRecord<String, String> record);

    // ── Async sends ───────────────────────────────────────────────────────────

    /** Non-blocking send — returns a future that completes when the broker acknowledges. */
    <T> CompletableFuture<SendResult<String, String>> sendAsync(String topic, T payload);

    /** Non-blocking keyed send. */
    <T> CompletableFuture<SendResult<String, String>> sendAsync(String topic, String key, T payload);

    // ── Batch send ────────────────────────────────────────────────────────────

    /**
     * Publish a list of payloads to the same topic in sequence.
     * Each item is sent individually — not a Kafka transactions batch.
     */
    <T> void sendBatch(String topic, List<T> payloads);

    // ── Transactional sends ───────────────────────────────────────────────────

    /**
     * Publish all payloads to {@code topic} atomically in a single Kafka transaction.
     * If any message fails to send, the entire transaction is aborted and
     * none of the messages will be visible to consumers.
     *
     * Requires {@code api.kafka.transaction.enabled=true} in application.properties.
     *
     * <pre>{@code
     * kafkaProducer.sendAllTransactional("order.events", List.of(event1, event2, event3, event4, event5));
     * }</pre>
     */
    <T> void sendAllTransactional(String topic, List<T> payloads);

    /**
     * Execute multiple publish operations (potentially to different topics)
     * within a single Kafka transaction. If any send inside the block throws,
     * the whole transaction is aborted — all or nothing.
     *
     * Requires {@code api.kafka.transaction.enabled=true} in application.properties.
     *
     * <pre>{@code
     * kafkaProducer.executeInTransaction(producer -> {
     *     producer.send("order.created",   orderEvent);
     *     producer.send("inventory.reserve", inventoryEvent);
     *     producer.send("payment.initiate",  paymentEvent);
     *     producer.send("notification.send", notificationEvent);
     *     producer.send("audit.log",         auditEvent);
     *     // if any of the above throw, ALL are rolled back
     * });
     * }</pre>
     */
    void executeInTransaction(Consumer<KafkaMessageProducer> actions);
}
