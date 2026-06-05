package com.bhagwat.scm.kafka.config;

import com.bhagwat.scm.kafka.dlt.DltEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DltEventHandler dltEventHandler;

    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(kafkaProperties.getConsumer().getConcurrency());
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    private CommonErrorHandler errorHandler() {
        KafkaProperties.Retry retry = kafkaProperties.getRetry();

        ExponentialBackOff backOff = new ExponentialBackOff(
                retry.getBackoffIntervalMs(),
                retry.getMultiplier());
        backOff.setMaxInterval(retry.getMaxIntervalMs());
        // maxAttempts includes the first attempt, so retries = maxAttempts - 1
        backOff.setMaxElapsedTime((long) (retry.getMaxAttempts() - 1) * retry.getMaxIntervalMs());

        if (kafkaProperties.getDlt().isEnabled()) {
            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                    kafkaTemplate,
                    (record, ex) -> {
                        // Route to <original-topic><suffix>
                        String dltTopic = record.topic() + kafkaProperties.getDlt().getTopicSuffix();
                        log.warn("Routing failed event to DLT: originalTopic={} dltTopic={} cause={}",
                                record.topic(), dltTopic, ex.getMessage());
                        dltEventHandler.handle(record, ex);
                        return new org.apache.kafka.common.TopicPartition(dltTopic, -1);
                    });

            DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
            handler.setRetryListeners((record, ex, deliveryAttempt) ->
                    log.warn("Retry attempt {}/{} for topic={} offset={} cause={}",
                            deliveryAttempt,
                            kafkaProperties.getRetry().getMaxAttempts(),
                            record.topic(),
                            record.offset(),
                            ex.getMessage()));
            return handler;
        }

        // DLT disabled — just retry with backoff, then log and skip
        DefaultErrorHandler handler = new DefaultErrorHandler(backOff);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt {}/{} for topic={} offset={} cause={}",
                        deliveryAttempt,
                        kafkaProperties.getRetry().getMaxAttempts(),
                        record.topic(),
                        record.offset(),
                        ex.getMessage()));
        return handler;
    }

    private Map<String, Object> consumerConfigs() {
        KafkaProperties.Consumer c = kafkaProperties.getConsumer();
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,            c.getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,   c.getAutoOffsetReset());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,    c.getMaxPollRecords());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,  c.isEnableAutoCommit());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,  c.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, c.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        applySecurity(props);
        return props;
    }

    private void applySecurity(Map<String, Object> props) {
        KafkaProperties.Security sec = kafkaProperties.getSecurity();
        if (!sec.isEnabled()) return;

        props.put("security.protocol", sec.getProtocol());
        props.put("sasl.mechanism",    sec.getSaslMechanism());
        props.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required "
                + "username=\"" + sec.getUsername() + "\" "
                + "password=\"" + sec.getPassword() + "\";");
    }
}
