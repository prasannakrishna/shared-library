package com.bhagwat.scm.kafka.consumer;

import com.bhagwat.scm.kafka.envelope.KafkaEventEnvelope;
import com.bhagwat.scm.kafka.tenant.NoOpTenantContextProvider;
import com.bhagwat.scm.kafka.tenant.TenantContextProvider;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

/**
 * Base class for all Kafka consumers in your service.
 *
 * Handles the full lifecycle automatically:
 * - JSON deserialisation into {@link KafkaEventEnvelope}
 * - Tenant context restoration before processing (from X-Tenant-Id header)
 * - Idempotency check (skips events already marked COMPLETED)
 * - State transitions: RECEIVED → PROCESSING → COMPLETED / FAILED
 * - Tenant context cleared in finally block — always clean after each message
 * - Structured logging at each state transition
 * - Exception propagation to trigger Spring Kafka's retry/DLT mechanism
 *
 * Usage:
 * <pre>{@code
 * @Component
 * public class InventoryEventConsumer extends AbstractKafkaEventConsumer<InventoryCreatedEvent> {
 *
 *     @Override
 *     protected void process(InventoryCreatedEvent payload, KafkaEventEnvelope<InventoryCreatedEvent> envelope) {
 *         // TenantContext is already set here — no manual setTenantFromHeader() needed
 *         inventoryRepo.save(...);
 *     }
 *
 *     @KafkaListener(topics = "inventory-created-topic", groupId = "${api.kafka.consumer.group-id}")
 *     public void consume(ConsumerRecord<String, String> record) {
 *         processRecord(record, InventoryCreatedEvent.class);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the business payload type inside the {@link KafkaEventEnvelope}
 */
@Slf4j
public abstract class AbstractKafkaEventConsumer<T> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventStateTracker eventStateTracker;

    /**
     * Optional — if no TenantContextProvider bean is in the context,
     * falls back to NoOpTenantContextProvider (no tenant propagation).
     */
    @Autowired(required = false)
    private TenantContextProvider tenantContextProvider;

    @PostConstruct
    private void initTenantProvider() {
        if (tenantContextProvider == null) {
            tenantContextProvider = new NoOpTenantContextProvider();
        }
    }

    /**
     * Entry point called from your {@code @KafkaListener} method.
     * Tenant context is restored before calling process() and cleared in the finally block.
     */
    protected void processRecord(ConsumerRecord<String, String> record, Class<T> payloadType) {
        KafkaEventEnvelope<T> envelope = null;

        try {
            JavaType envelopeType = objectMapper.getTypeFactory()
                    .constructParametricType(KafkaEventEnvelope.class, payloadType);
            envelope = objectMapper.readValue(record.value(), envelopeType);
        } catch (Exception e) {
            log.error("Failed to deserialize Kafka record from topic={} offset={}: {}",
                    record.topic(), record.offset(), e.getMessage(), e);
            throw new IllegalArgumentException(
                    "Deserialization failed for record at offset " + record.offset(), e);
        }

        String eventId = envelope.getEventId();
        String topic   = record.topic();

        // Idempotency guard
        if (eventStateTracker.isAlreadyProcessed(eventId)) {
            log.info("Skipping duplicate event: eventId={} topic={}", eventId, topic);
            return;
        }

        // Restore tenant context — extracted from envelope headers first,
        // then falls back to raw Kafka record headers (for legacy producers)
        String tenantId = resolveTenantId(record, envelope);
        if (tenantId != null) {
            tenantContextProvider.setTenantId(tenantId);
            log.debug("Tenant context restored: tenantId={} eventId={}", tenantId, eventId);
        } else {
            log.debug("No tenant found in event headers: eventId={} topic={}", eventId, topic);
        }

        try {
            log.info("Event RECEIVED: eventId={} type={} topic={} tenant={} retryCount={}",
                    eventId, envelope.getEventType(), topic, tenantId, envelope.getRetryCount());
            eventStateTracker.setState(eventId, EventProcessingState.RECEIVED);

            eventStateTracker.setState(eventId, EventProcessingState.PROCESSING);
            process(envelope.getPayload(), envelope);

            eventStateTracker.setState(eventId, EventProcessingState.COMPLETED);
            log.info("Event COMPLETED: eventId={} type={} topic={} tenant={}",
                    eventId, envelope.getEventType(), topic, tenantId);

        } catch (Exception e) {
            eventStateTracker.setState(eventId, EventProcessingState.FAILED);
            log.error("Event FAILED: eventId={} type={} topic={} tenant={} retryCount={} error={}",
                    eventId, envelope.getEventType(), topic, tenantId,
                    envelope.getRetryCount(), e.getMessage(), e);
            throw new RuntimeException("Event processing failed for eventId=" + eventId, e);

        } finally {
            // Always clear tenant context — prevents ThreadLocal leak across messages
            tenantContextProvider.clear();
        }
    }

    /**
     * Implement your business logic here.
     * The tenant context (TenantContext.getTenantId()) is already set when this is called.
     */
    protected abstract void process(T payload, KafkaEventEnvelope<T> envelope);

    // ── Tenant resolution ─────────────────────────────────────────────────────

    private String resolveTenantId(ConsumerRecord<String, String> record,
                                   KafkaEventEnvelope<T> envelope) {
        // 1. Check envelope headers (set by KafkaMessageProducerImpl automatically)
        String tenantId = envelope.getHeaders().get(TenantContextProvider.TENANT_HEADER);
        if (tenantId != null) return tenantId;

        // 2. Fall back to raw Kafka record headers (covers legacy producers not using the library)
        Header header = record.headers().lastHeader(TenantContextProvider.TENANT_HEADER);
        if (header != null) return new String(header.value(), StandardCharsets.UTF_8);

        return null;
    }
}
