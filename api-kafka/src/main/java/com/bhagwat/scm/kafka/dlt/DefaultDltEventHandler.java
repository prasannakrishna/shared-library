package com.bhagwat.scm.kafka.dlt;

import com.bhagwat.scm.kafka.consumer.EventProcessingState;
import com.bhagwat.scm.kafka.consumer.EventStateTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Default DLT handler — persists failed event to kafka_failed_events table
 * AND logs it. Provides full audit trail for ops investigation and replay.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDltEventHandler implements DltEventHandler {

    private final EventStateTracker eventStateTracker;
    private final FailedEventLogRepository failedEventLogRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AUDIT_TOPIC = "platform.failed-events";

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Override
    public void handle(ConsumerRecord<?, ?> record, Exception cause) {
        String topic = record.topic().replace(".DLT", ""); // strip DLT suffix to get original topic
        String key = record.key() != null ? record.key().toString() : null;
        String payload = record.value() != null ? record.value().toString() : null;

        // Extract event type from payload if possible
        String eventType = extractEventType(payload);
        int retryCount = extractRetryCount(record);

        // Log
        log.error("DLT FAILED EVENT: topic={} key={} eventType={} retries={} cause={}",
                topic, key, eventType, retryCount, cause.getMessage());
        log.error("DLT payload: {}", payload);

        // Persist to DB
        try {
            if (failedEventLogRepository != null) {
                FailedEventLog entry = FailedEventLog.builder()
                        .sourceTopic(topic)
                        .eventKey(key)
                        .eventType(eventType)
                        .payload(truncate(payload, 10000))
                        .errorClass(cause.getClass().getName())
                        .errorMessage(truncate(cause.getMessage(), 2000))
                        .stackTrace(truncate(getStackTrace(cause), 5000))
                        .retryCount(retryCount)
                        .consumerGroup(extractHeader(record, "kafka_consumerGroup"))
                        .partitionInfo("partition=" + record.partition() + " offset=" + record.offset())
                        .serviceName(serviceName)
                        .build();
                failedEventLogRepository.save(entry);
                log.info("DLT: Persisted failed event to kafka_failed_events: topic={} key={}", topic, key);
            }
        } catch (Exception e) {
            log.error("DLT: Failed to persist to kafka_failed_events: {}", e.getMessage());
        }

        // Mark in state tracker
        if (key != null) {
            eventStateTracker.setState(key, EventProcessingState.DEAD_LETTERED);
        }

        // Publish to central audit topic for auditService
        try {
            Map<String, Object> auditEvent = new java.util.HashMap<>();
            auditEvent.put("serviceName", serviceName);
            auditEvent.put("sourceTopic", topic);
            auditEvent.put("eventKey", key != null ? key : "");
            auditEvent.put("eventType", eventType);
            auditEvent.put("payload", payload != null ? truncate(payload, 10000) : "");
            auditEvent.put("errorClass", cause.getClass().getName());
            auditEvent.put("errorMessage", cause.getMessage() != null ? truncate(cause.getMessage(), 2000) : "");
            auditEvent.put("retryCount", retryCount);
            auditEvent.put("partition", record.partition());
            auditEvent.put("offset", record.offset());
            auditEvent.put("timestamp", Instant.now().toString());
            String auditJson = objectMapper.writeValueAsString(auditEvent);
            kafkaTemplate.send(AUDIT_TOPIC, serviceName + "|" + topic, auditJson);
        } catch (Exception e) {
            log.warn("Failed to publish to audit topic: {}", e.getMessage());
        }
    }

    private String extractEventType(String payload) {
        if (payload == null) return "UNKNOWN";
        try {
            Map<String, Object> map = objectMapper.readValue(payload, Map.class);
            Object type = map.get("eventType");
            return type != null ? type.toString() : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private int extractRetryCount(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader("kafka_dlt-original-offset");
        // Spring Kafka adds retry count header
        Header retryHeader = record.headers().lastHeader("kafka_dlt-exception-retryCount");
        if (retryHeader != null) {
            try { return Integer.parseInt(new String(retryHeader.value(), StandardCharsets.UTF_8)); }
            catch (Exception ignored) {}
        }
        return 0;
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header h = record.headers().lastHeader(headerName);
        return h != null ? new String(h.value(), StandardCharsets.UTF_8) : null;
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
