package com.bhagwat.scm.kafka.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for a single Kafka topic.
 * Defines partitions, retention, replication, and key strategy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicConfig {

    /** Number of partitions — determines max consumer parallelism */
    private int partitions;

    /** Retention in days — how long messages are kept */
    private int retentionDays;

    /** Replication factor — number of copies across brokers (min 1 for dev, 3 for prod) */
    @Builder.Default
    private short replicationFactor = 1;

    /** Key strategy — determines which field is used as the partition key */
    private KeyStrategy keyStrategy;

    /** Cleanup policy: "delete" (default) or "compact" for changelog topics */
    @Builder.Default
    private String cleanupPolicy = "delete";

    /** Max message size in bytes (default 1MB) */
    @Builder.Default
    private int maxMessageBytes = 1048576;

    /** Minimum in-sync replicas for acks=all to succeed */
    @Builder.Default
    private short minInsyncReplicas = 1;

    /**
     * Factory method for standard topics.
     */
    public static TopicConfig of(int partitions, int retentionDays, KeyStrategy keyStrategy) {
        return TopicConfig.builder()
                .partitions(partitions)
                .retentionDays(retentionDays)
                .keyStrategy(keyStrategy)
                .build();
    }

    /**
     * Factory method for compacted topics (changelog/state stores).
     */
    public static TopicConfig compacted(int partitions, KeyStrategy keyStrategy) {
        return TopicConfig.builder()
                .partitions(partitions)
                .retentionDays(-1) // infinite retention for compacted topics
                .keyStrategy(keyStrategy)
                .cleanupPolicy("compact")
                .build();
    }

    /**
     * Get retention in milliseconds (for Kafka admin config).
     */
    public long getRetentionMs() {
        if (retentionDays <= 0) return -1; // infinite
        return (long) retentionDays * 24 * 60 * 60 * 1000;
    }
}
