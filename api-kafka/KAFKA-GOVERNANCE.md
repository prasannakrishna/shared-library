# Kafka Governance — Enterprise Standards

## Topic Naming Convention

```
{domain}.{entity}.{action}
```

| Part | Values | Examples |
|------|--------|----------|
| domain | `inventory`, `catalog`, `order`, `transport`, `community`, `seller`, `feed`, `subscription`, `platform` | |
| entity | aggregate root noun | `product`, `sku`, `shipment`, `subscription`, `review` |
| action | past-tense verb | `created`, `updated`, `deleted`, `published`, `completed`, `delivered` |

**Examples:**
- `inventory.product.created` (not ~~product-created-topic~~)
- `transport.shipment.delivered` (already correct)
- `order.payment.success` (not ~~payment.success~~)

## Consumer Group Convention

```
{service-name}.{purpose}
```

**Examples:**
- `catalog-service.product-sync`
- `community-manager.product-indexer`
- `feed-service.pending-reviews`

**Rules:**
- Multiple instances of the same service share the same group ID (horizontal scaling)
- Different services consuming the same topic use different group IDs (fan-out)
- One group per logical consumer purpose (a service can have multiple groups)

## Partition Key Strategy

| Key Strategy | Use When | Guarantees |
|---|---|---|
| `PRODUCT_ID` | Events for the same product must be ordered | Create → Update → Delete in order |
| `ORDER_ID` | Order saga steps must not interleave | Checkout → Payment → Ship → Deliver |
| `SELLER_ID` | Seller-specific events | Work orders, reliability updates |
| `PRODUCT_SELLER` | Per product+seller (most inventory events) | Stock publish, costs |
| `CUSTOMER_ID` | Per customer actions | Subscriptions, memberships |
| `COMMUNITY_ID` | Per community | Posts, membership changes |
| `SHIPMENT_ID` | Shipment lifecycle ordering | Created → Milestone → Delivered |
| `NONE` | No ordering needed | Audit logs, DLT |

## Partition Count Guidelines

| Use Case | Partitions | Reasoning |
|----------|------------|-----------|
| High throughput (inventory, orders) | 6 | Allows 6 consumer instances max |
| Medium throughput (reviews, community) | 3 | 3 instances is enough |
| Low throughput (org events, DLT) | 1 | Single consumer is fine |
| Production (scale up) | 12-24 | For high-volume topics in prod |

## Retention Policy

| Category | Retention | Reasoning |
|----------|-----------|-----------|
| Delivery events (for audit) | 30 days | Legal compliance |
| Stock/order events | 14 days | Replay window for debugging |
| CRUD events | 7 days | Standard replay window |
| Platform audit (DLT, failed) | 90 days | Investigation window |
| Org events (tenant provisioning) | 30 days | Rare but important |

## DLT (Dead Letter Topic) Strategy

Every topic gets a `.DLT` counterpart automatically:
- `inventory.product.created` → `inventory.product.created.DLT`
- Retention: 90 days
- All DLT events also published to `platform.failed-events` (centralized audit)
- DLT events persisted to `kafka_failed_events` table in consuming service's DB

## Retry Configuration

```yaml
api:
  kafka:
    retry:
      max-attempts: 3          # 1 initial + 2 retries
      backoff-interval-ms: 1000
      multiplier: 2.0          # 1s → 2s → 4s
      max-interval-ms: 30000   # Cap at 30s
```

## Production Configuration

```yaml
# For 3-broker Kafka cluster:
api:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
    provisioner:
      enabled: true
      replication-factor: 3
      min-insync-replicas: 2
    producer:
      acks: all                # Wait for all replicas
      retries: 3
      delivery-timeout-ms: 120000
    consumer:
      concurrency: 3           # Match partition count / 2
      enable-auto-commit: false
      max-poll-records: 100
    transaction:
      enabled: true
      id-prefix: ${spring.application.name}-tx-
    security:
      enabled: true
      protocol: SASL_SSL
      sasl-mechanism: PLAIN
      username: ${KAFKA_USERNAME}
      password: ${KAFKA_PASSWORD}
```

## Usage in Services

```java
// Import topic constants:
import static com.bhagwat.scm.kafka.registry.TopicRegistry.*;
import static com.bhagwat.scm.kafka.registry.ConsumerGroups.*;

// Producer:
kafkaMessageProducer.send(INVENTORY_PRODUCT_CREATED, productId, event);

// Consumer:
@KafkaListener(topics = INVENTORY_PRODUCT_CREATED, groupId = CATALOG_PRODUCT_SYNC)
public void onProductCreated(ConsumerRecord<String, String> record) {
    processRecord(record, ProductCreatedEvent.class);
}
```

## Migration Path (Legacy → Standard)

1. Add `TopicRegistry` constants to shared library ✅
2. Deploy provisioner to create new topics ✅
3. Update producers to dual-write (old + new topic)
4. Update consumers to read from new topic + new group ID
5. Verify zero lag on new topics
6. Remove old topic from producer
7. Delete old topic after retention expires

Use `TopicMigrationMap.resolve("product-created-topic")` during transition.

## Monitoring Checklist

- [ ] Consumer lag per group (should be < 100 for real-time topics)
- [ ] DLT message count (should be 0 in steady state)
- [ ] Producer send latency p99
- [ ] Partition skew (no single partition getting 80%+ traffic)
- [ ] Under-replicated partitions (should be 0)
