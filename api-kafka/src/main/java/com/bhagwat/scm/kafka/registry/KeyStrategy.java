package com.bhagwat.scm.kafka.registry;

/**
 * Partition key strategies — determines which business field is used
 * as the Kafka message key for partition assignment.
 *
 * <h2>Why This Matters</h2>
 * Messages with the same key ALWAYS go to the same partition.
 * Within a partition, messages are strictly ordered.
 * So: same key = same partition = guaranteed ordering for that entity.
 *
 * <h2>Impact on Consumers</h2>
 * A consumer group with N consumers can process at most P partitions.
 * Max parallelism = min(partitions, consumers in group).
 *
 * <h2>Choosing the Right Strategy</h2>
 * <ul>
 *   <li>PRODUCT_ID — when events for the same product must be processed in order
 *       (create → update → delete)</li>
 *   <li>ORDER_ID — when the order saga steps must not interleave
 *       (checkout → payment → shipment → delivery)</li>
 *   <li>SELLER_ID — when all seller-specific events go together
 *       (work order completion, reliability updates)</li>
 *   <li>PRODUCT_SELLER — compound key for product+seller specific events
 *       (inventory publish, demand, cost)</li>
 *   <li>NONE — no ordering needed (audit logs, DLT)</li>
 * </ul>
 */
public enum KeyStrategy {

    /** Key = productId. Ordering per product. */
    PRODUCT_ID,

    /** Key = skuId. Ordering per SKU. */
    SKU_ID,

    /** Key = orderId. Ordering per order saga. */
    ORDER_ID,

    /** Key = sellerId. Ordering per seller. */
    SELLER_ID,

    /** Key = customerId. Ordering per customer. */
    CUSTOMER_ID,

    /** Key = communityId. Ordering per community. */
    COMMUNITY_ID,

    /** Key = shipmentId. Ordering per shipment lifecycle. */
    SHIPMENT_ID,

    /** Key = configId. Ordering per pack/catalog config. */
    CONFIG_ID,

    /** Compound key = productId|sellerId. For product+seller specific events. */
    PRODUCT_SELLER,

    /** No key — round-robin partition assignment. For unordered events. */
    NONE;

    /**
     * Resolve the actual key value from event fields.
     * Returns null for NONE (Kafka will round-robin).
     */
    public String resolveKey(String productId, String sellerId, String orderId,
                             String skuId, String customerId, String communityId,
                             String shipmentId, String configId) {
        return switch (this) {
            case PRODUCT_ID -> productId;
            case SKU_ID -> skuId;
            case ORDER_ID -> orderId;
            case SELLER_ID -> sellerId;
            case CUSTOMER_ID -> customerId;
            case COMMUNITY_ID -> communityId;
            case SHIPMENT_ID -> shipmentId;
            case CONFIG_ID -> configId;
            case PRODUCT_SELLER -> productId != null && sellerId != null
                    ? productId + "|" + sellerId : productId;
            case NONE -> null;
        };
    }
}
