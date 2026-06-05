package com.bhagwat.scm.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All Kafka configuration is driven from application.properties in the consuming service.
 *
 * Minimal required config:
 *   api.kafka.bootstrap-servers=localhost:9092
 *   api.kafka.consumer.group-id=my-service-group
 *
 * Full reference:
 *   api.kafka.bootstrap-servers=
 *   api.kafka.producer.acks=all
 *   api.kafka.producer.retries=3
 *   api.kafka.producer.batch-size=16384
 *   api.kafka.producer.linger-ms=1
 *   api.kafka.producer.request-timeout-ms=30000
 *   api.kafka.consumer.group-id=
 *   api.kafka.consumer.auto-offset-reset=earliest
 *   api.kafka.consumer.max-poll-records=100
 *   api.kafka.consumer.concurrency=3
 *   api.kafka.consumer.enable-auto-commit=false
 *   api.kafka.retry.max-attempts=3
 *   api.kafka.retry.backoff-interval-ms=1000
 *   api.kafka.retry.multiplier=2.0
 *   api.kafka.retry.max-interval-ms=30000
 *   api.kafka.dlt.enabled=true
 *   api.kafka.dlt.topic-suffix=.DLT
 *   api.kafka.security.enabled=false
 *   api.kafka.security.protocol=SASL_SSL
 *   api.kafka.security.sasl-mechanism=PLAIN
 *   api.kafka.security.username=
 *   api.kafka.security.password=
 */
@Data
@ConfigurationProperties(prefix = "api.kafka")
public class KafkaProperties {

    private String bootstrapServers = "localhost:9092";

    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    private Retry retry = new Retry();
    private Dlt dlt = new Dlt();
    private Transaction transaction = new Transaction();
    private Security security = new Security();

    @Data
    public static class Producer {
        private String acks = "all";
        private int retries = 3;
        private int batchSize = 16384;
        private int lingerMs = 1;
        private int requestTimeoutMs = 30000;
        private int deliveryTimeoutMs = 120000;
    }

    @Data
    public static class Consumer {
        private String groupId;
        private String autoOffsetReset = "earliest";
        private int maxPollRecords = 100;
        private int concurrency = 3;
        private boolean enableAutoCommit = false;
        private int sessionTimeoutMs = 30000;
        private int heartbeatIntervalMs = 3000;
    }

    @Data
    public static class Retry {
        /** Maximum delivery attempts before sending to DLT (includes first attempt). */
        private int maxAttempts = 3;
        /** Initial backoff in milliseconds between retry attempts. */
        private long backoffIntervalMs = 1000L;
        /** Exponential multiplier applied to backoff on each retry. */
        private double multiplier = 2.0;
        /** Upper ceiling for backoff interval in milliseconds. */
        private long maxIntervalMs = 30000L;
    }

    @Data
    public static class Dlt {
        private boolean enabled = true;
        /** Suffix appended to the original topic name to form the DLT topic. */
        private String topicSuffix = ".DLT";
    }

    @Data
    public static class Transaction {
        /**
         * Set to true to enable Kafka producer transactions.
         * Required for sendAllTransactional() and executeInTransaction() to work.
         *
         *   api.kafka.transaction.enabled=true
         *   api.kafka.transaction.id-prefix=my-service-tx-
         *
         * Each producer instance gets a unique ID: <idPrefix><sequence>
         * Ensure this prefix is unique per service to avoid transaction ID conflicts.
         */
        private boolean enabled = false;
        private String idPrefix = "tx-";
    }

    @Data
    public static class Security {
        private boolean enabled = false;
        private String protocol = "SASL_SSL";
        private String saslMechanism = "PLAIN";
        private String username;
        private String password;
    }
}
