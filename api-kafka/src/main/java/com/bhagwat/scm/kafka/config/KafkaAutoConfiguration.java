package com.bhagwat.scm.kafka.config;

import com.bhagwat.scm.kafka.consumer.EventStateTracker;
import com.bhagwat.scm.kafka.consumer.InMemoryEventStateTracker;
import com.bhagwat.scm.kafka.dlt.DefaultDltEventHandler;
import com.bhagwat.scm.kafka.dlt.DltEventHandler;
import com.bhagwat.scm.kafka.dlt.FailedEventLogRepository;
import com.bhagwat.scm.kafka.producer.KafkaMessageProducer;
import com.bhagwat.scm.kafka.producer.KafkaMessageProducerImpl;
import com.bhagwat.scm.kafka.tenant.NoOpTenantContextProvider;
import com.bhagwat.scm.kafka.tenant.TenantContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.transaction.KafkaTransactionManager;

/**
 * Central auto-configuration for the api-kafka library.
 *
 * Activated either via:
 *   1. {@code @EnableKafkaMessaging} on your main class
 *   2. Automatic Spring Boot auto-configuration (META-INF/spring/...imports)
 *
 * All beans are conditional on missing — override any of them in your service
 * if you need custom behaviour.
 */
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaAutoConfiguration {

    // ── ObjectMapper ──────────────────────────────────────────────────────────

    @Bean("kafkaObjectMapper")
    @ConditionalOnMissingBean(name = "kafkaObjectMapper")
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // ── Event State Tracker ───────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean(EventStateTracker.class)
    public EventStateTracker eventStateTracker() {
        return new InMemoryEventStateTracker();
    }

    // ── DLT Handler ───────────────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean(DltEventHandler.class)
    public DltEventHandler dltEventHandler(EventStateTracker eventStateTracker,
                                           org.springframework.beans.factory.ObjectProvider<FailedEventLogRepository> repoProvider,
                                           com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                           org.springframework.beans.factory.ObjectProvider<KafkaTemplate> kafkaTemplateProvider) {
        FailedEventLogRepository repo = repoProvider.getIfAvailable();
        KafkaTemplate kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        return new DefaultDltEventHandler(eventStateTracker, repo, objectMapper, kafkaTemplate);
    }

    // ── Tenant Context Provider ───────────────────────────────────────────────
    // Registers the no-op default so the producer/consumer always have something to inject.
    // Services that use multi-tenancy override this with a real implementation that
    // delegates to their TenantContext (ThreadLocal, MDC, etc.).

    @Bean
    @ConditionalOnMissingBean(TenantContextProvider.class)
    public TenantContextProvider tenantContextProvider() {
        return new NoOpTenantContextProvider();
    }

    // ── Producer ─────────────────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public KafkaProducerConfig kafkaProducerConfig(KafkaProperties kafkaProperties) {
        return new KafkaProducerConfig(kafkaProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, String> kafkaTemplate(KafkaProducerConfig kafkaProducerConfig) {
        return kafkaProducerConfig.kafkaTemplate();
    }

    @Bean
    @ConditionalOnMissingBean(KafkaMessageProducer.class)
    public KafkaMessageProducer kafkaMessageProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper kafkaObjectMapper,
            TenantContextProvider tenantContextProvider) {
        return new KafkaMessageProducerImpl(kafkaTemplate, kafkaObjectMapper, tenantContextProvider);
    }

    // ── Consumer container factory ────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public KafkaConsumerConfig kafkaConsumerConfig(
            KafkaProperties kafkaProperties,
            KafkaTemplate<String, String> kafkaTemplate,
            DltEventHandler dltEventHandler) {
        return new KafkaConsumerConfig(kafkaProperties, kafkaTemplate, dltEventHandler);
    }

    @Bean("kafkaListenerContainerFactory")
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaConsumerConfig kafkaConsumerConfig) {
        return kafkaConsumerConfig.kafkaListenerContainerFactory();
    }

    // ── Kafka Transaction Manager ─────────────────────────────────────────────
    // Registered only when api.kafka.transaction.enabled=true.
    // Enables @Transactional on service methods that publish multiple Kafka messages —
    // if the method throws, all sends within that transaction are rolled back.

    @Bean
    @ConditionalOnMissingBean(KafkaTransactionManager.class)
    @ConditionalOnProperty(prefix = "api.kafka.transaction", name = "enabled", havingValue = "true")
    public KafkaTransactionManager<String, String> kafkaTransactionManager(
            KafkaProducerConfig kafkaProducerConfig) {
        return new KafkaTransactionManager<>(kafkaProducerConfig.producerFactory());
    }
}
