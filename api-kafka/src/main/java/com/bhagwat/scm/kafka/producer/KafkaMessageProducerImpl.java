package com.bhagwat.scm.kafka.producer;

import com.bhagwat.scm.kafka.envelope.KafkaEventEnvelope;
import com.bhagwat.scm.kafka.exception.KafkaPublishException;
import com.bhagwat.scm.kafka.tenant.TenantContextProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class KafkaMessageProducerImpl implements KafkaMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TenantContextProvider tenantContextProvider;

    // ── Simple sends ──────────────────────────────────────────────────────────

    @Override
    public <T> void send(String topic, T payload) {
        String json = serialize(payload);
        doSend(topic, null, json);
        log.info("Kafka message sent: topic={} payloadType={} tenant={}",
                topic, payload.getClass().getSimpleName(), tenantContextProvider.getTenantId());
    }

    @Override
    public <T> void send(String topic, String key, T payload) {
        String json = serialize(payload);
        doSend(topic, key, json);
        log.info("Kafka message sent: topic={} key={} payloadType={} tenant={}",
                topic, key, payload.getClass().getSimpleName(), tenantContextProvider.getTenantId());
    }

    // ── Envelope sends ────────────────────────────────────────────────────────

    @Override
    public <T> void sendEnvelope(String topic, KafkaEventEnvelope<T> envelope) {
        injectTenantIntoEnvelope(envelope);
        String json = serialize(envelope);
        doSend(topic, null, json);
        log.info("Kafka envelope sent: topic={} eventId={} type={} tenant={}",
                topic, envelope.getEventId(), envelope.getEventType(),
                envelope.getHeaders().get(TenantContextProvider.TENANT_HEADER));
    }

    @Override
    public <T> void sendEnvelopeKeyed(String topic, KafkaEventEnvelope<T> envelope) {
        injectTenantIntoEnvelope(envelope);
        String json = serialize(envelope);
        doSend(topic, envelope.getEventId(), json);
        log.info("Kafka envelope sent (keyed): topic={} eventId={} type={} tenant={}",
                topic, envelope.getEventId(), envelope.getEventType(),
                envelope.getHeaders().get(TenantContextProvider.TENANT_HEADER));
    }

    // ── Raw ProducerRecord ────────────────────────────────────────────────────

    @Override
    public void sendRecord(ProducerRecord<String, String> record) {
        // Stamp tenant on the raw record too if available
        stampTenantHeader(record);
        try {
            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send ProducerRecord to topic={}: {}", record.topic(), ex.getMessage(), ex);
                    throw new KafkaPublishException("Failed to send record to topic: " + record.topic(), ex);
                }
                log.debug("ProducerRecord sent: topic={} partition={} offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }).get();
        } catch (KafkaPublishException e) {
            throw e;
        } catch (Exception e) {
            throw new KafkaPublishException("Failed to send ProducerRecord to topic: " + record.topic(), e);
        }
    }

    // ── Async sends ───────────────────────────────────────────────────────────

    @Override
    public <T> CompletableFuture<SendResult<String, String>> sendAsync(String topic, T payload) {
        String json = serialize(payload);
        ProducerRecord<String, String> record = buildRecord(topic, null, json);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        attachLoggingCallback(future, topic, null);
        return future;
    }

    @Override
    public <T> CompletableFuture<SendResult<String, String>> sendAsync(String topic, String key, T payload) {
        String json = serialize(payload);
        ProducerRecord<String, String> record = buildRecord(topic, key, json);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        attachLoggingCallback(future, topic, key);
        return future;
    }

    // ── Batch send ────────────────────────────────────────────────────────────

    @Override
    public <T> void sendBatch(String topic, List<T> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            log.warn("sendBatch called with empty list for topic={}", topic);
            return;
        }
        log.info("Sending batch of {} messages to topic={} tenant={}",
                payloads.size(), topic, tenantContextProvider.getTenantId());
        payloads.forEach(payload -> send(topic, payload));
    }

    // ── Transactional sends ───────────────────────────────────────────────────

    @Override
    public <T> void sendAllTransactional(String topic, List<T> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            log.warn("sendAllTransactional called with empty list for topic={}", topic);
            return;
        }
        String tenantId = tenantContextProvider.getTenantId();
        log.info("Starting Kafka transaction: {} messages to topic={} tenant={}", payloads.size(), topic, tenantId);
        try {
            kafkaTemplate.executeInTransaction(ops -> {
                payloads.forEach(payload -> {
                    ProducerRecord<String, String> record = buildRecord(topic, null, serialize(payload));
                    ops.send(record);
                    log.debug("Transactional send queued: topic={} payloadType={} tenant={}",
                            topic, payload.getClass().getSimpleName(), tenantId);
                });
                return true;
            });
            log.info("Kafka transaction committed: {} messages to topic={} tenant={}",
                    payloads.size(), topic, tenantId);
        } catch (Exception e) {
            log.error("Kafka transaction aborted: all {} messages to topic={} rolled back. tenant={} cause={}",
                    payloads.size(), topic, tenantId, e.getMessage(), e);
            throw new KafkaPublishException(
                    "Transactional publish failed — all " + payloads.size()
                    + " messages to topic '" + topic + "' were rolled back.", e);
        }
    }

    @Override
    public void executeInTransaction(Consumer<KafkaMessageProducer> actions) {
        String tenantId = tenantContextProvider.getTenantId();
        log.info("Starting Kafka transaction block. tenant={}", tenantId);
        try {
            kafkaTemplate.executeInTransaction(ops -> {
                actions.accept(this);
                return true;
            });
            log.info("Kafka transaction block committed. tenant={}", tenantId);
        } catch (KafkaPublishException e) {
            log.error("Kafka transaction block aborted — all messages rolled back. tenant={} cause={}",
                    tenantId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Kafka transaction block aborted — all messages rolled back. tenant={} cause={}",
                    tenantId, e.getMessage(), e);
            throw new KafkaPublishException("Transactional publish block failed — all messages rolled back.", e);
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /**
     * Core send — always uses ProducerRecord so we can stamp the tenant header.
     */
    private void doSend(String topic, String key, String json) {
        try {
            ProducerRecord<String, String> record = buildRecord(topic, key, json);
            kafkaTemplate.send(record).get();
        } catch (Exception e) {
            throw new KafkaPublishException("Failed to publish to topic: " + topic, e);
        }
    }

    /**
     * Build a ProducerRecord and stamp X-Tenant-Id if a tenant is present in context.
     */
    private ProducerRecord<String, String> buildRecord(String topic, String key, String json) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, key, json);
        stampTenantHeader(record);
        return record;
    }

    private void stampTenantHeader(ProducerRecord<String, String> record) {
        String tenantId = tenantContextProvider.getTenantId();
        if (tenantId != null) {
            record.headers().add(
                    TenantContextProvider.TENANT_HEADER,
                    tenantId.getBytes(StandardCharsets.UTF_8));
            log.debug("Stamped tenant header: topic={} tenantId={}", record.topic(), tenantId);
        }
    }

    private <T> void injectTenantIntoEnvelope(KafkaEventEnvelope<T> envelope) {
        String tenantId = tenantContextProvider.getTenantId();
        if (tenantId != null && !envelope.getHeaders().containsKey(TenantContextProvider.TENANT_HEADER)) {
            envelope.getHeaders().put(TenantContextProvider.TENANT_HEADER, tenantId);
        }
    }

    private <T> String serialize(T payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new KafkaPublishException(
                    "Failed to serialize payload: " + payload.getClass().getSimpleName(), e);
        }
    }

    private void attachLoggingCallback(CompletableFuture<SendResult<String, String>> future,
                                       String topic, String key) {
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Async send failed: topic={} key={} error={}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Async send succeeded: topic={} key={} partition={} offset={}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
