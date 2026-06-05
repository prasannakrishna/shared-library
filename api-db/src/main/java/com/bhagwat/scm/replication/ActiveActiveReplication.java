package com.bhagwat.scm.replication;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Application-Level Active-Active Replication using PostgreSQL + Kafka.
 *
 * Architecture:
 *   Node 1 (Mumbai)                    Node 2 (Singapore)
 *   ┌──────────────┐                  ┌──────────────┐
 *   │ PostgreSQL   │                  │ PostgreSQL   │
 *   │ (full copy)  │                  │ (full copy)  │
 *   └──────┬───────┘                  └──────┬───────┘
 *          │                                  │
 *          ▼                                  ▼
 *   ┌──────────────┐    Kafka Topic    ┌──────────────┐
 *   │ App writes   │──── repl.events ─►│ App replays  │
 *   │ locally      │◄── repl.events ───│ writes       │
 *   └──────────────┘                  └──────────────┘
 *
 * How it works:
 * 1. App writes to LOCAL PostgreSQL
 * 2. After commit, publishes the change to Kafka topic "db.replication.events"
 * 3. Other region's app consumes the event and replays it on ITS local PostgreSQL
 * 4. Conflict resolution: Last-Writer-Wins (LWW) using timestamp
 *
 * Usage:
 *    ReplicationPublisher publisher;
 *
 *   // After saving to local DB:
 *   customerRepository.save(customer);
 *   publisher.publishChange("customers", customer.getId().toString(), "UPSERT", Map.of(
 *       "id", customer.getId(), "username", customer.getUsername(), "email", customer.getEmail(), ...
 *   ));
 */
@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "replication.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveActiveReplication {

    private final KafkaTemplate kafkaTemplate;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Value("${replication.node-id:node-1}")
    private String nodeId;

    @Value("${replication.topic:db.replication.events}")
    private String replicationTopic;

    // ── Publisher: Send local changes to Kafka ───────────────────────────

    /**
     * Publish a database change event after local commit.
     * Call this AFTER your repository.save() succeeds.
     */
    public void publishChange(String tableName, String recordId, String operation, Map<String, Object> data) {
        ReplicationEvent event = ReplicationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceNodeId(nodeId)
                .tableName(tableName)
                .recordId(recordId)
                .operation(operation) // UPSERT, DELETE
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        kafkaTemplate.send(replicationTopic, recordId, event);
        log.debug("Replication event published: {} {} on {}", operation, recordId, tableName);
    }

    // ── Consumer: Replay remote changes locally ──────────────────────────

    @KafkaListener(topics = "${replication.topic:db.replication.events}", groupId = "${replication.consumer-group:repl-${replication.node-id:node-1}}")
    public void onReplicationEvent(Map<String, Object> raw) {
        try {
            String sourceNode = (String) raw.get("sourceNodeId");

            // Skip events from self (avoid infinite loop)
            if (nodeId.equals(sourceNode)) return;

            String tableName = (String) raw.get("tableName");
            String recordId = (String) raw.get("recordId");
            String operation = (String) raw.get("operation");
            long remoteTimestamp = ((Number) raw.get("timestamp")).longValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) raw.get("data");

            // Conflict resolution: Last-Writer-Wins
            if (isLocalNewer(tableName, recordId, remoteTimestamp)) {
                log.debug("Skipping stale replication event for {}/{} — local is newer", tableName, recordId);
                return;
            }

            // Replay the change locally
            if ("DELETE".equals(operation)) {
                executeDelete(tableName, recordId);
            } else {
                executeUpsert(tableName, recordId, data, remoteTimestamp);
            }

            log.info("Replicated from {}: {} {}/{}", sourceNode, operation, tableName, recordId);
        } catch (Exception e) {
            log.error("Replication replay failed: {}", e.getMessage());
        }
    }

    // ── Conflict Resolution: Last-Writer-Wins ────────────────────────────

    private boolean isLocalNewer(String tableName, String recordId, long remoteTimestamp) {
        try (Connection conn = dataSource.getConnection()) {
            // Check local updated_at timestamp
            String sql = "SELECT EXTRACT(EPOCH FROM updated_at) * 1000 FROM " + sanitize(tableName) + " WHERE id = ?::uuid";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, recordId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long localTimestamp = rs.getLong(1);
                    return localTimestamp > remoteTimestamp;
                }
            }
        } catch (Exception e) {
            // Record doesn't exist locally — remote is "newer"
        }
        return false;
    }

    // ── SQL Execution ────────────────────────────────────────────────────

    private void executeUpsert(String tableName, String recordId, Map<String, Object> data, long timestamp) throws SQLException {
        if (data == null || data.isEmpty()) return;

        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        StringBuilder updates = new StringBuilder();

        for (String col : data.keySet()) {
            String safeCol = sanitize(col);
            if (cols.length() > 0) { cols.append(","); vals.append(","); updates.append(","); }
            cols.append(safeCol);
            vals.append("?");
            updates.append(safeCol).append("=EXCLUDED.").append(safeCol);
        }

        String sql = String.format(
                "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (id) DO UPDATE SET %s",
                sanitize(tableName), cols, vals, updates);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object val : data.values()) {
                ps.setObject(i++, val);
            }
            ps.executeUpdate();
        }
    }

    private void executeDelete(String tableName, String recordId) throws SQLException {
        String sql = "DELETE FROM " + sanitize(tableName) + " WHERE id = ?::uuid";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recordId);
            ps.executeUpdate();
        }
    }

    private String sanitize(String input) {
        // Prevent SQL injection — only allow alphanumeric + underscore
        return input.replaceAll("[^a-zA-Z0-9_]", "");
    }

    @Data @Builder
    public static class ReplicationEvent {
        private String eventId;
        private String sourceNodeId;
        private String tableName;
        private String recordId;
        private String operation; // UPSERT, DELETE
        private Map<String, Object> data;
        private long timestamp; // Epoch millis — used for LWW conflict resolution
    }
}
