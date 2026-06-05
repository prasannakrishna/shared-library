# api-kafka

A production-ready Kafka wrapper library for Spring Boot microservices.
Provides a clean abstraction over Spring Kafka with built-in support for:

- Typed message publishing (simple, keyed, envelope, batch)
- Consumer lifecycle management with event state tracking
- Configurable retry with exponential backoff
- Dead Letter Topic (DLT) routing with custom handlers
- Kafka Transactions (all-or-nothing publish)
- Partition-level control via ProducerRecord
- SASL security configuration
- Full property-driven configuration — zero Java config needed in consuming services

---

## Table of Contents

1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [Configuration Reference](#configuration-reference)
4. [Publishing Messages](#publishing-messages)
5. [Consuming Messages](#consuming-messages)
6. [Event Envelope](#event-envelope)
7. [Event State Tracking](#event-state-tracking)
8. [Retry Configuration](#retry-configuration)
9. [Dead Letter Topic (DLT)](#dead-letter-topic-dlt)
10. [Kafka Transactions](#kafka-transactions)
11. [Partition-Level Control](#partition-level-control)
12. [Security (SASL)](#security-sasl)
13. [Overriding Default Behaviour](#overriding-default-behaviour)
14. [Complete Configuration Reference](#complete-configuration-reference)

---

## Installation

Add the dependency to your service's `build.gradle`:

```groovy
implementation 'com.bhagwat.scm:api-kafka:1.0.0-SNAPSHOT'
```

Ensure `mavenLocal()` is in your repositories (the library is published locally):

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}
```

---

## Quick Start

### 1. Enable the library

Add `@EnableKafkaMessaging` to your Spring Boot main class:

```java
@SpringBootApplication
@EnableKafkaMessaging
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 2. Minimum required configuration

In your `application.properties`:

```properties
api.kafka.bootstrap-servers=localhost:9092
api.kafka.consumer.group-id=order-service-group
```

### 3. Publish a message

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaMessageProducer kafkaProducer;

    public void placeOrder(Order order) {
        kafkaProducer.send("order.created", order);
    }
}
```

### 4. Consume a message

```java
@Component
public class OrderEventConsumer extends AbstractKafkaEventConsumer<Order> {

    @Override
    protected void process(Order payload, KafkaEventEnvelope<Order> envelope) {
        // your business logic here
        System.out.println("Processing order: " + payload.getOrderId());
    }

    @KafkaListener(topics = "order.created", groupId = "${api.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        processRecord(record, Order.class);
    }
}
```

That's it. Retry, DLT routing, state tracking, and logging are all handled automatically.

---

## Configuration Reference

All configuration is read from the consuming service's `application.properties`.
No Java configuration class is needed.

```properties
# ── Broker ────────────────────────────────────────────────────────────────────
api.kafka.bootstrap-servers=localhost:9092

# ── Producer ──────────────────────────────────────────────────────────────────
api.kafka.producer.acks=all                  # all = strongest durability guarantee
api.kafka.producer.retries=3                 # producer-level retries (network errors)
api.kafka.producer.batch-size=16384          # bytes
api.kafka.producer.linger-ms=1              # wait time before sending a batch
api.kafka.producer.request-timeout-ms=30000
api.kafka.producer.delivery-timeout-ms=120000

# ── Consumer ──────────────────────────────────────────────────────────────────
api.kafka.consumer.group-id=my-service-group
api.kafka.consumer.auto-offset-reset=earliest
api.kafka.consumer.max-poll-records=100
api.kafka.consumer.concurrency=3             # parallel listener threads
api.kafka.consumer.enable-auto-commit=false  # always false — library manages commits
api.kafka.consumer.session-timeout-ms=30000
api.kafka.consumer.heartbeat-interval-ms=3000

# ── Retry ─────────────────────────────────────────────────────────────────────
api.kafka.retry.max-attempts=3               # total attempts (1 original + 2 retries)
api.kafka.retry.backoff-interval-ms=1000     # initial backoff
api.kafka.retry.multiplier=2.0               # exponential growth factor
api.kafka.retry.max-interval-ms=30000        # backoff ceiling

# ── Dead Letter Topic ─────────────────────────────────────────────────────────
api.kafka.dlt.enabled=true
api.kafka.dlt.topic-suffix=.DLT              # DLT topic = original-topic + suffix

# ── Transactions ──────────────────────────────────────────────────────────────
api.kafka.transaction.enabled=false          # set true for atomic multi-message publish
api.kafka.transaction.id-prefix=my-service-tx-  # must be unique per service

# ── Security (SASL) ───────────────────────────────────────────────────────────
api.kafka.security.enabled=false
api.kafka.security.protocol=SASL_SSL
api.kafka.security.sasl-mechanism=PLAIN
api.kafka.security.username=
api.kafka.security.password=
```

---

## Publishing Messages

Inject `KafkaMessageProducer` into any Spring bean.

### Simple send (no key)

```java
kafkaProducer.send("order.created", orderEvent);
```

The payload is serialised to JSON automatically.

### Keyed send (for partition ordering)

Messages with the same key are guaranteed to land on the same partition,
preserving order for that key.

```java
// All events for orderId "ORD-123" go to the same partition — ordered delivery
kafkaProducer.send("order.events", "ORD-123", orderEvent);
```

### Envelope send (recommended for production)

The `KafkaEventEnvelope` wraps your payload with tracing metadata:
`eventId`, `correlationId`, `eventType`, `timestamp`, `headers`, `retryCount`.

```java
KafkaEventEnvelope<OrderEvent> envelope = KafkaEventEnvelope.<OrderEvent>builder()
        .eventType("OrderCreated")
        .correlationId(requestContext.getCorrelationId())
        .source("order-service")
        .payload(orderEvent)
        .headers(Map.of("region", "EU", "priority", "HIGH"))
        .build();

kafkaProducer.sendEnvelope("order.created", envelope);
```

Use `sendEnvelopeKeyed()` to also use the `eventId` as the partition key:

```java
kafkaProducer.sendEnvelopeKeyed("order.created", envelope);
```

### Async send (fire-and-forget)

```java
CompletableFuture<SendResult<String, String>> future =
        kafkaProducer.sendAsync("order.created", orderEvent);

// Optional: attach a callback
future.whenComplete((result, ex) -> {
    if (ex != null) {
        log.error("Failed to publish order event", ex);
    } else {
        log.info("Published to partition={} offset={}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }
});
```

### Batch send

Sends each item individually in sequence (not transactional — use `sendAllTransactional` for atomicity):

```java
List<OrderEvent> events = List.of(event1, event2, event3);
kafkaProducer.sendBatch("order.created", events);
```

---

## Consuming Messages

Extend `AbstractKafkaEventConsumer<T>` in your consumer class.
The base class handles:

- JSON deserialisation into `KafkaEventEnvelope<T>`
- Idempotency check (skips events already marked COMPLETED)
- State transitions: `RECEIVED → PROCESSING → COMPLETED / FAILED`
- Structured logging at each transition
- Exception propagation to trigger Spring Kafka's retry/DLT mechanism

```java
@Component
@RequiredArgsConstructor
public class OrderEventConsumer extends AbstractKafkaEventConsumer<OrderCreatedEvent> {

    private final OrderRepository orderRepository;

    @Override
    protected void process(OrderCreatedEvent payload, KafkaEventEnvelope<OrderCreatedEvent> envelope) {
        log.info("Processing order: orderId={} correlationId={}",
                payload.getOrderId(), envelope.getCorrelationId());

        orderRepository.save(Order.from(payload));
    }

    @KafkaListener(
        topics = "${app.topics.order-created}",
        groupId = "${api.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        processRecord(record, OrderCreatedEvent.class);
    }
}
```

### Idempotency

Before calling `process()`, the base class calls `eventStateTracker.isAlreadyProcessed(eventId)`.
If the event was previously COMPLETED (e.g. duplicate delivery), it is skipped silently.

```java
// This will be skipped automatically if already processed
@KafkaListener(topics = "order.created", groupId = "${api.kafka.consumer.group-id}")
public void consume(ConsumerRecord<String, String> record) {
    processRecord(record, OrderCreatedEvent.class);   // idempotency handled internally
}
```

### Consuming plain messages (without envelope)

If you are consuming from a topic that does NOT use `KafkaEventEnvelope`
(e.g. a third-party topic), handle it directly without extending the base class:

```java
@Component
public class ExternalEventConsumer {

    @KafkaListener(topics = "external.payments", groupId = "${api.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        // parse record.value() yourself
        log.info("Received external event: {}", record.value());
    }
}
```

---

## Event Envelope

`KafkaEventEnvelope<T>` is the standard event wrapper used by all envelope-based sends.

| Field | Type | Description |
|---|---|---|
| `eventId` | `String` | Auto-generated UUID. Used for idempotency. |
| `eventType` | `String` | Logical event name e.g. `"OrderCreated"` |
| `correlationId` | `String` | Trace ID from upstream service |
| `source` | `String` | Publishing service name |
| `timestamp` | `Instant` | UTC time of event creation |
| `payload` | `T` | Your business object |
| `retryCount` | `int` | How many times this event has been retried |
| `headers` | `Map<String,String>` | Arbitrary metadata |

```java
// Reading envelope fields in your consumer
@Override
protected void process(OrderCreatedEvent payload, KafkaEventEnvelope<OrderCreatedEvent> envelope) {
    String eventId       = envelope.getEventId();
    String correlationId = envelope.getCorrelationId();
    int    retryCount    = envelope.getRetryCount();
    String region        = envelope.getHeaders().get("region");
}
```

---

## Event State Tracking

`EventStateTracker` records the lifecycle state of each event by its `eventId`.

### States

| State | Meaning |
|---|---|
| `RECEIVED` | Event pulled from Kafka, not yet in business logic |
| `PROCESSING` | Business logic is executing |
| `COMPLETED` | Successfully processed — duplicate deliveries are skipped |
| `FAILED` | Exception thrown — Spring Kafka will retry |
| `DEAD_LETTERED` | All retries exhausted — event routed to DLT |

### Querying state programmatically

```java
@Autowired
private EventStateTracker eventStateTracker;

// Check current state
Optional<EventProcessingState> state = eventStateTracker.getState(eventId);

// Check if already completed (idempotency guard)
boolean done = eventStateTracker.isAlreadyProcessed(eventId);

// Manually update state
eventStateTracker.setState(eventId, EventProcessingState.COMPLETED);

// Remove tracking entry
eventStateTracker.remove(eventId);
```

### Default implementation

The default `InMemoryEventStateTracker` uses a `ConcurrentHashMap`.
This is suitable for single-instance services or local development.

### Production: Redis-backed state tracker

For distributed deployments (multiple replicas), override the bean
to use Redis for shared state across instances:

```java
@Bean
@Primary
public EventStateTracker redisEventStateTracker(RedisClient redisClient) {
    return new EventStateTracker() {

        private static final String PREFIX = "kafka:event:state:";
        private static final Duration TTL  = Duration.ofHours(24);

        @Override
        public void setState(String eventId, EventProcessingState state) {
            redisClient.set(PREFIX + eventId, state.name(), TTL);
        }

        @Override
        public Optional<EventProcessingState> getState(String eventId) {
            Object val = redisClient.get(PREFIX + eventId);
            return Optional.ofNullable(val)
                    .map(v -> EventProcessingState.valueOf(v.toString()));
        }

        @Override
        public boolean isAlreadyProcessed(String eventId) {
            return getState(eventId)
                    .map(s -> s == EventProcessingState.COMPLETED)
                    .orElse(false);
        }

        @Override
        public void remove(String eventId) {
            redisClient.delete(PREFIX + eventId);
        }
    };
}
```

---

## Retry Configuration

The library uses Spring Kafka's `DefaultErrorHandler` with `ExponentialBackOff`.

```properties
api.kafka.retry.max-attempts=3          # 1 original + 2 retries
api.kafka.retry.backoff-interval-ms=1000
api.kafka.retry.multiplier=2.0
api.kafka.retry.max-interval-ms=30000
```

With the above config, retry timing is:

| Attempt | Delay before retry |
|---|---|
| 1st retry | 1 second |
| 2nd retry | 2 seconds |
| → DLT | (max attempts reached) |

### What happens on failure

1. Your `process()` method throws an exception
2. The base class catches it, marks state as `FAILED`, and rethrows
3. Spring Kafka intercepts the exception and schedules a retry
4. On each retry, a warning is logged: `Retry attempt X/Y for topic=...`
5. After `max-attempts`, the message is routed to the DLT topic

---

## Dead Letter Topic (DLT)

When all retry attempts are exhausted, the failed message is published to
`<original-topic><dlt-suffix>` (default: `<original-topic>.DLT`).

```properties
api.kafka.dlt.enabled=true
api.kafka.dlt.topic-suffix=.DLT
```

For example: `order.created` → `order.created.DLT`

### Consuming the DLT topic

Set up a listener on the DLT topic to inspect or reprocess failed events:

```java
@Component
public class OrderDltConsumer {

    @KafkaListener(
        topics = "order.created.DLT",
        groupId = "${api.kafka.consumer.group-id}-dlt"
    )
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.error("DLT: Failed event received. topic={} key={} error={}",
                record.topic(), record.key(), errorMessage);
        // Options: persist to DB, raise alert, manual requeue, discard
    }
}
```

### Custom DLT handler

Override the default DLT handler to add custom logic (alerting, DB persistence, etc.):

```java
@Component
public class AlertingDltHandler implements DltEventHandler {

    private final AlertService alertService;
    private final FailedEventRepository failedEventRepository;

    @Override
    public void handle(ConsumerRecord<?, ?> record, Exception cause) {
        // Persist the failed event
        failedEventRepository.save(FailedEvent.builder()
                .topic(record.topic())
                .payload(record.value().toString())
                .errorMessage(cause.getMessage())
                .failedAt(Instant.now())
                .build());

        // Trigger alert
        alertService.sendCriticalAlert(
                "Kafka DLT: event failed on topic " + record.topic(), cause);
    }
}
```

Providing this bean automatically replaces `DefaultDltEventHandler`.

---

## Kafka Transactions

Transactions guarantee **all-or-nothing** publishing.
If any message in the transaction fails to send, the entire set is rolled back —
none of the messages will be visible to consumers.

### Enable transactions

```properties
api.kafka.transaction.enabled=true
api.kafka.transaction.id-prefix=order-service-tx-
```

> The `id-prefix` must be **unique per service**. If two services share a prefix,
> transactions can interfere with each other at the broker level.

### Option 1 — Same topic, same payload type

```java
List<OrderEvent> events = List.of(event1, event2, event3, event4, event5);

// All 5 published atomically — if event 3 fails, events 1 and 2 are rolled back
kafkaProducer.sendAllTransactional("order.events", events);
```

### Option 2 — Multiple topics / different payload types

```java
kafkaProducer.executeInTransaction(producer -> {
    producer.send("order.created",      new OrderCreatedEvent(order));
    producer.send("inventory.reserved", new InventoryReservedEvent(order));
    producer.send("payment.initiated",  new PaymentInitiatedEvent(order));
    producer.send("notification.send",  new NotificationEvent(order));
    producer.send("audit.log",          new AuditEvent(order));
    // Any exception here → ALL 5 messages are rolled back
});
```

### Option 3 — Declarative `@Transactional` (cleanest for service methods)

```java
@Service
@RequiredArgsConstructor
public class OrderOrchestrationService {

    private final KafkaMessageProducer kafkaProducer;

    @Transactional("kafkaTransactionManager")
    public void orchestrateOrderFlow(Order order) {
        kafkaProducer.send("order.created",      new OrderCreatedEvent(order));
        kafkaProducer.send("inventory.reserved", new InventoryReservedEvent(order));
        kafkaProducer.send("payment.initiated",  new PaymentInitiatedEvent(order));
        kafkaProducer.send("notification.send",  new NotificationEvent(order));
        kafkaProducer.send("audit.log",          new AuditEvent(order));
        // RuntimeException thrown here → all 5 rolled back automatically
    }
}
```

### Combining Kafka + DB transactions (dual-write pattern)

Use `ChainedKafkaTransactionManager` to keep Kafka and database commits in sync:

```java
@Bean
public ChainedKafkaTransactionManager<String, String> chainedTransactionManager(
        KafkaTransactionManager<String, String> kafkaTransactionManager,
        PlatformTransactionManager jpaTransactionManager) {
    return new ChainedKafkaTransactionManager<>(kafkaTransactionManager, jpaTransactionManager);
}
```

Then use `@Transactional("chainedTransactionManager")` — if the DB commit fails,
the Kafka transaction is also rolled back.

### Consumer isolation (important)

To prevent consumers from seeing uncommitted transactional messages,
set `isolation.level=read_committed` in your consumer config:

```properties
spring.kafka.consumer.properties.isolation.level=read_committed
```

---

## Partition-Level Control

Use `sendRecord()` with a fully constructed `ProducerRecord` when you need
to target a specific partition, set custom Kafka headers, or set a timestamp.

### Send to a specific partition

```java
// Force all VIP order events to partition 0 for priority processing
ProducerRecord<String, String> record = new ProducerRecord<>(
        "order.created",    // topic
        0,                  // partition
        "ORD-VIP-999",      // key
        objectMapper.writeValueAsString(orderEvent)  // value (JSON)
);

kafkaProducer.sendRecord(record);
```

### Send with custom Kafka headers

```java
ProducerRecord<String, String> record = new ProducerRecord<>(
        "order.created",
        null,               // partition — null means use key-based partitioner
        "ORD-123",          // key
        objectMapper.writeValueAsString(orderEvent)
);

// Add Kafka-level headers (visible to consumers via @Header)
record.headers()
        .add("X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8))
        .add("X-Source-Service", "order-service".getBytes(StandardCharsets.UTF_8))
        .add("X-Event-Version",  "v2".getBytes(StandardCharsets.UTF_8));

kafkaProducer.sendRecord(record);
```

Reading the headers in the consumer:

```java
@KafkaListener(topics = "order.created", groupId = "${api.kafka.consumer.group-id}")
public void consume(
        ConsumerRecord<String, String> record,
        @Header("X-Correlation-Id") String correlationId,
        @Header("X-Event-Version")  String eventVersion) {
    log.info("Received with correlationId={} version={}", correlationId, eventVersion);
}
```

### Send with a specific timestamp

```java
ProducerRecord<String, String> record = new ProducerRecord<>(
        "order.created",
        null,                       // partition
        Instant.now().toEpochMilli(), // custom timestamp
        "ORD-123",
        objectMapper.writeValueAsString(orderEvent)
);
kafkaProducer.sendRecord(record);
```

### Partition assignment strategy

Without a key, messages are distributed round-robin across all partitions.
With a key, Kafka hashes the key to consistently assign the same partition —
this is the recommended approach for ordering guarantees per entity:

```java
// All events for the same orderId always go to the same partition
kafkaProducer.send("order.events", order.getOrderId(), orderEvent);
```

### Controlling concurrency per partition

Match `concurrency` to the number of partitions for maximum throughput.
Each thread processes one partition:

```properties
# If your topic has 6 partitions:
api.kafka.consumer.concurrency=6
```

---

## Security (SASL)

For production brokers requiring SASL authentication:

```properties
api.kafka.security.enabled=true
api.kafka.security.protocol=SASL_SSL
api.kafka.security.sasl-mechanism=PLAIN
api.kafka.security.username=${KAFKA_USERNAME}
api.kafka.security.password=${KAFKA_PASSWORD}
```

For `SCRAM-SHA-256` or `SCRAM-SHA-512`:

```properties
api.kafka.security.sasl-mechanism=SCRAM-SHA-256
```

---

## Overriding Default Behaviour

All core beans are declared with `@ConditionalOnMissingBean`.
Declare your own bean of the same type in your service to override any default.

### Custom DLT handler

```java
@Component
public class MyDltHandler implements DltEventHandler {
    @Override
    public void handle(ConsumerRecord<?, ?> record, Exception cause) {
        // custom logic
    }
}
```

### Custom event state tracker (Redis, DB, etc.)

```java
@Bean
@Primary
public EventStateTracker myEventStateTracker() {
    return new MyDatabaseEventStateTracker();
}
```

### Custom Kafka ObjectMapper

The library uses a dedicated `kafkaObjectMapper` bean (separate from your app's `ObjectMapper`)
to avoid polluting global serialisation config. Override it if needed:

```java
@Bean("kafkaObjectMapper")
public ObjectMapper kafkaObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
}
```

### Custom listener container factory

```java
@Bean("kafkaListenerContainerFactory")
public ConcurrentKafkaListenerContainerFactory<String, String> myFactory(
        ConsumerFactory<String, String> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    // custom config
    return factory;
}
```

---

## Module Structure

```
api-kafka/
└── src/main/java/com/bhagwat/scm/kafka/
    ├── annotation/
    │   └── EnableKafkaMessaging.java       ← add to @SpringBootApplication
    ├── config/
    │   ├── KafkaProperties.java            ← all api.kafka.* properties
    │   ├── KafkaAutoConfiguration.java     ← wires all beans
    │   ├── KafkaProducerConfig.java        ← producer factory + template
    │   └── KafkaConsumerConfig.java        ← consumer factory + retry + DLT
    ├── producer/
    │   ├── KafkaMessageProducer.java       ← interface
    │   └── KafkaMessageProducerImpl.java   ← implementation
    ├── consumer/
    │   ├── AbstractKafkaEventConsumer.java ← extend this in your service
    │   ├── EventProcessingState.java       ← RECEIVED/PROCESSING/COMPLETED/FAILED/DEAD_LETTERED
    │   ├── EventStateTracker.java          ← interface
    │   └── InMemoryEventStateTracker.java  ← default in-memory impl
    ├── dlt/
    │   ├── DltEventHandler.java            ← interface
    │   └── DefaultDltEventHandler.java     ← logs + marks DEAD_LETTERED
    ├── envelope/
    │   └── KafkaEventEnvelope.java         ← event wrapper with metadata
    └── exception/
        └── KafkaPublishException.java
```

---

## Dependency

```
Group:    com.bhagwat.scm
Artifact: api-kafka
Version:  1.0.0-SNAPSHOT
```
