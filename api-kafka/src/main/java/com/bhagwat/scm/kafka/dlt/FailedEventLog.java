package com.bhagwat.scm.kafka.dlt;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Persistent audit log for Kafka events that failed after all retries + DLT.
 * Stored in each service's DB (public schema) for ops visibility and replay.
 *
 * Table: kafka_failed_events
 */
@Entity
@Table(name = "kafka_failed_events", schema = "public")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FailedEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Original topic the event was consumed from */
    @Column(name = "source_topic", nullable = false)
    private String sourceTopic;

    /** Kafka message key */
    @Column(name = "event_key")
    private String eventKey;

    /** Event type from envelope (e.g., ORG_CREATED, INVENTORY_RESERVE_REQUEST) */
    @Column(name = "event_type")
    private String eventType;

    /** Full payload (JSON) */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /** Root cause exception class */
    @Column(name = "error_class")
    private String errorClass;

    /** Error message */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Full stack trace */
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    /** Number of retry attempts before DLT */
    @Column(name = "retry_count")
    private Integer retryCount;

    /** Consumer group that failed */
    @Column(name = "consumer_group")
    private String consumerGroup;

    /** Partition + offset for replay reference */
    @Column(name = "partition_info")
    private String partitionInfo;

    /** Service that failed to process */
    @Column(name = "service_name")
    private String serviceName;

    /** PENDING (needs investigation), REPLAYED, IGNORED */
    @Column(name = "resolution_status", length = 20)
    @Builder.Default
    private String resolutionStatus = "PENDING";

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
