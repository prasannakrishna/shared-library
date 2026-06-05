package com.bhagwat.scm.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    public ProducerFactory<String, String> producerFactory() {
        DefaultKafkaProducerFactory<String, String> factory =
                new DefaultKafkaProducerFactory<>(producerConfigs());
        // Enable transactions if configured — required for sendAllTransactional()
        if (kafkaProperties.getTransaction().isEnabled()) {
            factory.setTransactionIdPrefix(kafkaProperties.getTransaction().getIdPrefix());
        }
        return factory;
    }

    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private Map<String, Object> producerConfigs() {
        KafkaProperties.Producer p = kafkaProperties.getProducer();
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,    kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,  StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG,                  p.getAcks());
        props.put(ProducerConfig.RETRIES_CONFIG,               p.getRetries());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,            p.getBatchSize());
        props.put(ProducerConfig.LINGER_MS_CONFIG,             p.getLingerMs());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,    p.getRequestTimeoutMs());
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,   p.getDeliveryTimeoutMs());

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
