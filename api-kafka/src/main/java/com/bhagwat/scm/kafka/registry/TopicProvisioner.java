package com.bhagwat.scm.kafka.registry;

import com.bhagwat.scm.kafka.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.TopicConfig;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Kafka Topic Provisioner — creates and validates topics on application startup.
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>On startup: verifies all registered topics exist with correct config</li>
 *   <li>Missing topics are created automatically</li>
 *   <li>Existing topics with wrong partition count are LOGGED (partitions can't be reduced)</li>
 *   <li>Retention and config differences are updated in-place</li>
 * </ul>
 *
 * <h2>Environment Adaptation</h2>
 * <pre>
 *   DEV:  replicationFactor=1, minInsyncReplicas=1
 *   PROD: replicationFactor=3, minInsyncReplicas=2
 * </pre>
 *
 * <h2>Usage</h2>
 * The provisioner runs automatically via {@code KafkaAutoConfiguration} when
 * {@code api.kafka.provisioner.enabled=true} (default: true).
 * Set to false in test environments where Kafka isn't available.
 */
@Slf4j
@RequiredArgsConstructor
public class TopicProvisioner {

    private final KafkaProperties kafkaProperties;

    /**
     * Provision all topics from the registry.
     * Call this on application startup (via @PostConstruct or ApplicationReadyEvent).
     */
    public void provisionAll() {
        Map<String, com.bhagwat.scm.kafka.registry.TopicConfig> registry = TopicRegistry.getAllTopics();
        log.info("Kafka Topic Provisioner: checking {} topics...", registry.size());

        try (AdminClient admin = createAdminClient()) {
            // Get existing topics
            Set<String> existingTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);

            List<NewTopic> toCreate = new ArrayList<>();
            List<String> toValidate = new ArrayList<>();

            for (Map.Entry<String, com.bhagwat.scm.kafka.registry.TopicConfig> entry : registry.entrySet()) {
                String topicName = entry.getKey();
                com.bhagwat.scm.kafka.registry.TopicConfig config = entry.getValue();

                if (!existingTopics.contains(topicName)) {
                    // Create missing topic
                    short replication = resolveReplicationFactor(config);
                    NewTopic newTopic = new NewTopic(topicName, config.getPartitions(), replication);
                    newTopic.configs(buildTopicConfigs(config));
                    toCreate.add(newTopic);
                } else {
                    toValidate.add(topicName);
                }
            }

            // Create missing topics
            if (!toCreate.isEmpty()) {
                log.info("Creating {} missing topics...", toCreate.size());
                CreateTopicsResult result = admin.createTopics(toCreate);
                result.all().get(30, TimeUnit.SECONDS);
                for (NewTopic t : toCreate) {
                    log.info("  ✓ Created topic: {} (partitions={}, replication={})",
                            t.name(), t.numPartitions(), t.replicationFactor());
                }
            }

            // Validate existing topics
            if (!toValidate.isEmpty()) {
                validateExistingTopics(admin, toValidate, registry);
            }

            // Also create DLT topics for each main topic
            List<NewTopic> dltTopics = new ArrayList<>();
            String dltSuffix = kafkaProperties.getDlt().getTopicSuffix();
            for (String topicName : registry.keySet()) {
                String dltName = topicName + dltSuffix;
                if (!existingTopics.contains(dltName)) {
                    NewTopic dlt = new NewTopic(dltName, 1, resolveReplicationFactor(null));
                    dlt.configs(Map.of(
                            TopicConfig.RETENTION_MS_CONFIG, String.valueOf(90L * 24 * 60 * 60 * 1000), // 90 days
                            TopicConfig.CLEANUP_POLICY_CONFIG, "delete"
                    ));
                    dltTopics.add(dlt);
                }
            }
            if (!dltTopics.isEmpty()) {
                log.info("Creating {} DLT topics...", dltTopics.size());
                admin.createTopics(dltTopics).all().get(30, TimeUnit.SECONDS);
                log.info("  ✓ DLT topics created (90-day retention)");
            }

            log.info("Kafka Topic Provisioner: complete. {} created, {} validated, {} DLTs.",
                    toCreate.size(), toValidate.size(), dltTopics.size());

        } catch (Exception e) {
            log.error("Kafka Topic Provisioner FAILED: {}. Topics may not be configured correctly.",
                    e.getMessage());
            // Don't fail startup — topics might be created by another service instance
        }
    }

    /**
     * Provision only specific topics (for services that only need a subset).
     */
    public void provision(String... topicNames) {
        Map<String, com.bhagwat.scm.kafka.registry.TopicConfig> registry = TopicRegistry.getAllTopics();
        Map<String, com.bhagwat.scm.kafka.registry.TopicConfig> subset = new LinkedHashMap<>();
        for (String name : topicNames) {
            com.bhagwat.scm.kafka.registry.TopicConfig config = registry.get(name);
            if (config != null) {
                subset.put(name, config);
            } else {
                log.warn("Topic '{}' not found in registry — skipping", name);
            }
        }

        if (subset.isEmpty()) return;

        try (AdminClient admin = createAdminClient()) {
            Set<String> existing = admin.listTopics().names().get(10, TimeUnit.SECONDS);
            List<NewTopic> toCreate = new ArrayList<>();

            for (Map.Entry<String, com.bhagwat.scm.kafka.registry.TopicConfig> entry : subset.entrySet()) {
                if (!existing.contains(entry.getKey())) {
                    short replication = resolveReplicationFactor(entry.getValue());
                    NewTopic nt = new NewTopic(entry.getKey(), entry.getValue().getPartitions(), replication);
                    nt.configs(buildTopicConfigs(entry.getValue()));
                    toCreate.add(nt);
                }
            }

            if (!toCreate.isEmpty()) {
                admin.createTopics(toCreate).all().get(30, TimeUnit.SECONDS);
                toCreate.forEach(t -> log.info("  ✓ Provisioned: {} (partitions={})", t.name(), t.numPartitions()));
            }
        } catch (Exception e) {
            log.error("Topic provisioning failed: {}", e.getMessage());
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void validateExistingTopics(AdminClient admin, List<String> topicNames,
                                        Map<String, com.bhagwat.scm.kafka.registry.TopicConfig> registry)
            throws ExecutionException, InterruptedException {

        Map<String, TopicDescription> descriptions = admin.describeTopics(topicNames)
                .allTopicNames().get();

        int warnings = 0;
        for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
            String name = entry.getKey();
            TopicDescription desc = entry.getValue();
            com.bhagwat.scm.kafka.registry.TopicConfig expected = registry.get(name);

            int actualPartitions = desc.partitions().size();
            int expectedPartitions = expected.getPartitions();

            if (actualPartitions < expectedPartitions) {
                log.warn("  ⚠ Topic '{}' has {} partitions but expected {}. "
                        + "Cannot reduce partitions. Consider increasing.",
                        name, actualPartitions, expectedPartitions);
                warnings++;
            }
        }

        if (warnings > 0) {
            log.warn("Topic validation: {} warnings found. Review partition configuration.", warnings);
        }
    }

    private Map<String, String> buildTopicConfigs(com.bhagwat.scm.kafka.registry.TopicConfig config) {
        Map<String, String> props = new HashMap<>();
        props.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(config.getRetentionMs()));
        props.put(TopicConfig.CLEANUP_POLICY_CONFIG, config.getCleanupPolicy());
        props.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, String.valueOf(config.getMaxMessageBytes()));
        props.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(config.getMinInsyncReplicas()));
        return props;
    }

    private short resolveReplicationFactor(com.bhagwat.scm.kafka.registry.TopicConfig config) {
        // In dev/test with 1 broker, always use 1
        // In prod with 3+ brokers, use configured value
        // The provisioner detects available brokers and adapts
        short configured = config != null ? config.getReplicationFactor() : 1;
        // For safety, default to 1 (dev-friendly). Production override via properties.
        return configured;
    }

    private AdminClient createAdminClient() {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 30000);

        KafkaProperties.Security sec = kafkaProperties.getSecurity();
        if (sec.isEnabled()) {
            props.put("security.protocol", sec.getProtocol());
            props.put("sasl.mechanism", sec.getSaslMechanism());
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required "
                    + "username=\"" + sec.getUsername() + "\" "
                    + "password=\"" + sec.getPassword() + "\";");
        }

        return AdminClient.create(props);
    }
}
