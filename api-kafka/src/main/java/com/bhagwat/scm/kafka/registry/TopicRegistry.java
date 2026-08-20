package com.bhagwat.scm.kafka.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized Kafka Topic Registry — Single source of truth for all topics.
 *
 * <h2>Naming Convention</h2>
 * <pre>
 *   {domain}.{entity}.{action}
 *
 *   domain   = bounded context (inventory, catalog, order, transport, community, seller, feed, platform)
 *   entity   = aggregate root (product, sku, shipment, subscription, review)
 *   action   = lifecycle verb (created, updated, deleted, synced, completed, failed)
 * </pre>
 *
 * <h2>Examples</h2>
 * <pre>
 *   inventory.product.created     (not "product-created-topic")
 *   transport.shipment.delivered  (already correct)
 *   order.payment.success         (not "payment.success")
 *   seller.workorder.completed    (already correct)
 * </pre>
 *
 * <h2>Partition Key Strategy</h2>
 * Each topic defines a key strategy that guarantees ordering per business entity:
 * <ul>
 *   <li>PRODUCT_ID — all events for the same product go to the same partition</li>
 *   <li>SELLER_ID — all events for the same seller go to the same partition</li>
 *   <li>ORDER_ID — all events for the same order go to the same partition</li>
 *   <li>SKU_ID — all events for the same SKU go to the same partition</li>
 *   <li>COMMUNITY_ID — all events for the same community go to the same partition</li>
 *   <li>SHIPMENT_ID — all events for the same shipment go to the same partition</li>
 *   <li>CUSTOMER_ID — all events for the same customer go to the same partition</li>
 * </ul>
 *
 * <h2>Consumer Group Convention</h2>
 * <pre>
 *   {consuming-service}.{purpose}
 *
 *   e.g. catalog-service.product-sync
 *        community-manager.product-affinity
 *        feed-service.review-creation
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   // In producer:
 *   kafkaProducer.send(TopicRegistry.INVENTORY_PRODUCT_CREATED, productId, event);
 *
 *   // In consumer:
 *   @KafkaListener(topics = TopicRegistry.INVENTORY_PRODUCT_CREATED,
 *                  groupId = ConsumerGroups.CATALOG_PRODUCT_SYNC)
 * }</pre>
 */
public final class TopicRegistry {

    private TopicRegistry() {}

    // ══════════════════════════════════════════════════════════════════════════
    // INVENTORY DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Product lifecycle events (created/updated/deleted) from sellerService → downstream */
    public static final String INVENTORY_PRODUCT_CREATED = "inventory.product.created";
    public static final String INVENTORY_PRODUCT_UPDATED = "inventory.product.updated";
    public static final String INVENTORY_PRODUCT_DELETED = "inventory.product.deleted";

    /** SKU lifecycle events */
    public static final String INVENTORY_SKU_CREATED = "inventory.sku.created";
    public static final String INVENTORY_SKU_UPDATED = "inventory.sku.updated";
    public static final String INVENTORY_SKU_DELETED = "inventory.sku.deleted";

    /** Inventory stock events (published/adjusted/allocated) */
    public static final String INVENTORY_STOCK_PUBLISHED = "inventory.stock.published";
    public static final String INVENTORY_STOCK_ADJUSTED = "inventory.stock.adjusted";
    public static final String INVENTORY_STOCK_ALLOCATED = "inventory.stock.allocated";

    /** Seller → Central inventory sync */
    public static final String INVENTORY_SELLER_SYNC = "inventory.seller.sync";

    /** Workflow completion events affecting inventory readiness */
    public static final String INVENTORY_WORKFLOW_COMPLETED = "inventory.workflow.completed";

    // ══════════════════════════════════════════════════════════════════════════
    // CATALOG DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Catalog pack config events (for sellerService sync) */
    public static final String CATALOG_PACKCONFIG_CREATED = "catalog.packconfig.created";
    public static final String CATALOG_PACKCONFIG_UPDATED = "catalog.packconfig.updated";

    /** Catalog SKU events */
    public static final String CATALOG_SKU_PUBLISHED = "catalog.sku.published";

    // ══════════════════════════════════════════════════════════════════════════
    // ORDER DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Cart checkout → order grouping */
    public static final String ORDER_CART_CHECKOUT = "order.cart.checkout";

    /** Payment lifecycle */
    public static final String ORDER_PAYMENT_SUCCESS = "order.payment.success";
    public static final String ORDER_PAYMENT_FAILED = "order.payment.failed";

    /** Order delivery events (triggers trial unlock, pending reviews) */
    public static final String ORDER_DELIVERY_COMPLETED = "order.delivery.completed";

    /** Shipping order assignment */
    public static final String ORDER_SHIPPING_ASSIGNED = "order.shipping.assigned";

    // ══════════════════════════════════════════════════════════════════════════
    // TRANSPORT DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Ready-to-ship created → transport planner */
    public static final String TRANSPORT_RTS_CREATED = "transport.rts.created";

    /** Shipment lifecycle */
    public static final String TRANSPORT_SHIPMENT_CREATED = "transport.shipment.created";
    public static final String TRANSPORT_SHIPMENT_MILESTONE = "transport.shipment.milestone";
    public static final String TRANSPORT_SHIPMENT_DELIVERED = "transport.shipment.delivered";

    // ══════════════════════════════════════════════════════════════════════════
    // SELLER DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Work order completion (triggers catalog reliability update + auto-publish) */
    public static final String SELLER_WORKORDER_COMPLETED = "seller.workorder.completed";

    /** Seller SKU local events (for MongoDB mirror sync) */
    public static final String SELLER_SKU_LOCAL = "seller.sku.local";

    /** Seller pack config local events */
    public static final String SELLER_PACKCONFIG_LOCAL = "seller.packconfig.local";

    /** Unit cost updates */
    public static final String SELLER_UNITCOST_UPDATED = "seller.unitcost.updated";

    /** Organization lifecycle (tenant provisioning) */
    public static final String SELLER_ORG_EVENTS = "seller.org.events";

    // ══════════════════════════════════════════════════════════════════════════
    // COMMUNITY DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Community lifecycle */
    public static final String COMMUNITY_CREATED = "community.created";
    public static final String COMMUNITY_UPDATED = "community.updated";

    /** Community membership changes */
    public static final String COMMUNITY_MEMBERSHIP_CHANGED = "community.membership.changed";

    // ══════════════════════════════════════════════════════════════════════════
    // SUBSCRIPTION DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Subscription lifecycle (created/activated/cancelled/hold/resume) */
    public static final String SUBSCRIPTION_ACTIVATED = "subscription.activated";
    public static final String SUBSCRIPTION_CANCELLED = "subscription.cancelled";
    public static final String SUBSCRIPTION_HOLD = "subscription.hold";
    public static final String SUBSCRIPTION_RESUMED = "subscription.resumed";

    /** Demand aggregation (from subscriptions → catalog capacity planning) */
    public static final String SUBSCRIPTION_DEMAND_AGGREGATED = "subscription.demand.aggregated";

    // ══════════════════════════════════════════════════════════════════════════
    // FEED / REVIEW DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Feed post lifecycle */
    public static final String FEED_POST_CREATED = "feed.post.created";

    /** Product review submitted (impacts catalog ranking + community scoring) */
    public static final String FEED_REVIEW_SUBMITTED = "feed.review.submitted";

    // ══════════════════════════════════════════════════════════════════════════
    // PLATFORM DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Equilibrium signals (catalog → seller agent) */
    public static final String PLATFORM_EQUILIBRIUM_SIGNAL = "platform.equilibrium.signal";

    /** Centralized failed event audit trail */
    public static final String PLATFORM_FAILED_EVENTS = "platform.failed-events";

    /** Analytics data gap events (NLQ misses → auditService) */
    public static final String PLATFORM_DATA_GAPS = "platform.data-gaps";

    /** Database replication (active-active) */
    public static final String PLATFORM_DB_REPLICATION = "platform.db.replication";

    /** Space allocation events */
    public static final String PLATFORM_SPACE_EVENTS = "platform.space.events";

    // ══════════════════════════════════════════════════════════════════════════
    // FINANCE DOMAIN
    // ══════════════════════════════════════════════════════════════════════════

    /** Invoice lifecycle events (status changes for data-platform + audit) */
    public static final String FINANCE_INVOICE_EVENTS = "finance.invoice.events";

    /** Contract lifecycle events (created/activated/expired/renewed) */
    public static final String FINANCE_CONTRACT_EVENTS = "contract.lifecycle.events";

    // ══════════════════════════════════════════════════════════════════════════
    // HUB OPERATIONS DOMAIN (Cross-dock coordination)
    // ══════════════════════════════════════════════════════════════════════════

    /** Shipment arrived at cross-dock hub (transportPlanner → wmsService) */
    public static final String HUB_INBOUND_ARRIVED = "hub.inbound.arrived";

    /** Hub processing complete, consignment ready for outbound (wmsService → transportPlanner) */
    public static final String HUB_OUTBOUND_READY = "hub.outbound.ready";

    /** Hub processing timeout escalation (transportPlanner → notificationService) */
    public static final String HUB_PROCESSING_DELAYED = "hub.processing.delayed";

    // ══════════════════════════════════════════════════════════════════════════
    // ORDER/DEMAND DOMAIN (unified order management)
    // ══════════════════════════════════════════════════════════════════════════

    /** Customer/Community order created (orderService → analytics) */
    public static final String ORDER_CREATED = "order.created";

    /** Order status changed (orderService → all tenant services) */
    public static final String ORDER_UPDATED = "order.updated";

    /** Order allocated (orderService → analytics) */
    public static final String ORDER_ALLOCATED = "order.allocated";

    /** Order delivered/fulfilled (orderService → analytics) */
    public static final String ORDER_FULFILLED = "order.fulfilled";

    /** Fulfillment order created post-allocation (orderService → store/wms/seller) */
    public static final String FULFILLMENT_ORDER_CREATED = "fulfillment.order.created";

    /** Replenishment order created after approval (orderService → wms/seller) */
    public static final String REPLENISHMENT_ORDER_CREATED = "replenishment.order.created";

    // ══════════════════════════════════════════════════════════════════════════
    // TOPIC METADATA (for provisioning)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns all topic definitions with their configuration.
     * Used by TopicProvisioner to create/validate topics.
     */
    public static Map<String, TopicConfig> getAllTopics() {
        Map<String, TopicConfig> topics = new LinkedHashMap<>();

        // Inventory domain (high throughput, keyed by productId/skuId)
        topics.put(INVENTORY_PRODUCT_CREATED, TopicConfig.of(6, 7, KeyStrategy.PRODUCT_ID));
        topics.put(INVENTORY_PRODUCT_UPDATED, TopicConfig.of(6, 7, KeyStrategy.PRODUCT_ID));
        topics.put(INVENTORY_PRODUCT_DELETED, TopicConfig.of(3, 7, KeyStrategy.PRODUCT_ID));
        topics.put(INVENTORY_SKU_CREATED, TopicConfig.of(6, 7, KeyStrategy.SKU_ID));
        topics.put(INVENTORY_SKU_UPDATED, TopicConfig.of(6, 7, KeyStrategy.SKU_ID));
        topics.put(INVENTORY_SKU_DELETED, TopicConfig.of(3, 7, KeyStrategy.SKU_ID));
        topics.put(INVENTORY_STOCK_PUBLISHED, TopicConfig.of(6, 14, KeyStrategy.PRODUCT_SELLER));
        topics.put(INVENTORY_STOCK_ADJUSTED, TopicConfig.of(6, 14, KeyStrategy.SKU_ID));
        topics.put(INVENTORY_STOCK_ALLOCATED, TopicConfig.of(6, 7, KeyStrategy.ORDER_ID));
        topics.put(INVENTORY_SELLER_SYNC, TopicConfig.of(6, 7, KeyStrategy.SELLER_ID));
        topics.put(INVENTORY_WORKFLOW_COMPLETED, TopicConfig.of(3, 7, KeyStrategy.PRODUCT_ID));

        // Catalog domain
        topics.put(CATALOG_PACKCONFIG_CREATED, TopicConfig.of(3, 7, KeyStrategy.CONFIG_ID));
        topics.put(CATALOG_PACKCONFIG_UPDATED, TopicConfig.of(3, 7, KeyStrategy.CONFIG_ID));
        topics.put(CATALOG_SKU_PUBLISHED, TopicConfig.of(3, 7, KeyStrategy.SKU_ID));

        // Order domain (keyed by orderId for saga ordering)
        topics.put(ORDER_CART_CHECKOUT, TopicConfig.of(6, 14, KeyStrategy.ORDER_ID));
        topics.put(ORDER_PAYMENT_SUCCESS, TopicConfig.of(6, 14, KeyStrategy.ORDER_ID));
        topics.put(ORDER_PAYMENT_FAILED, TopicConfig.of(3, 14, KeyStrategy.ORDER_ID));
        topics.put(ORDER_DELIVERY_COMPLETED, TopicConfig.of(6, 14, KeyStrategy.ORDER_ID));
        topics.put(ORDER_SHIPPING_ASSIGNED, TopicConfig.of(6, 7, KeyStrategy.ORDER_ID));

        // Transport domain (keyed by shipmentId)
        topics.put(TRANSPORT_RTS_CREATED, TopicConfig.of(3, 7, KeyStrategy.ORDER_ID));
        topics.put(TRANSPORT_SHIPMENT_CREATED, TopicConfig.of(6, 7, KeyStrategy.SHIPMENT_ID));
        topics.put(TRANSPORT_SHIPMENT_MILESTONE, TopicConfig.of(6, 14, KeyStrategy.SHIPMENT_ID));
        topics.put(TRANSPORT_SHIPMENT_DELIVERED, TopicConfig.of(6, 30, KeyStrategy.SHIPMENT_ID));

        // Seller domain
        topics.put(SELLER_WORKORDER_COMPLETED, TopicConfig.of(3, 14, KeyStrategy.SELLER_ID));
        topics.put(SELLER_SKU_LOCAL, TopicConfig.of(3, 7, KeyStrategy.SKU_ID));
        topics.put(SELLER_PACKCONFIG_LOCAL, TopicConfig.of(3, 7, KeyStrategy.CONFIG_ID));
        topics.put(SELLER_UNITCOST_UPDATED, TopicConfig.of(3, 7, KeyStrategy.PRODUCT_SELLER));
        topics.put(SELLER_ORG_EVENTS, TopicConfig.of(1, 30, KeyStrategy.SELLER_ID));

        // Community domain
        topics.put(COMMUNITY_CREATED, TopicConfig.of(3, 30, KeyStrategy.COMMUNITY_ID));
        topics.put(COMMUNITY_UPDATED, TopicConfig.of(3, 7, KeyStrategy.COMMUNITY_ID));
        topics.put(COMMUNITY_MEMBERSHIP_CHANGED, TopicConfig.of(3, 7, KeyStrategy.CUSTOMER_ID));

        // Subscription domain
        topics.put(SUBSCRIPTION_ACTIVATED, TopicConfig.of(6, 14, KeyStrategy.CUSTOMER_ID));
        topics.put(SUBSCRIPTION_CANCELLED, TopicConfig.of(3, 14, KeyStrategy.CUSTOMER_ID));
        topics.put(SUBSCRIPTION_HOLD, TopicConfig.of(3, 7, KeyStrategy.CUSTOMER_ID));
        topics.put(SUBSCRIPTION_RESUMED, TopicConfig.of(3, 7, KeyStrategy.CUSTOMER_ID));
        topics.put(SUBSCRIPTION_DEMAND_AGGREGATED, TopicConfig.of(3, 7, KeyStrategy.PRODUCT_SELLER));

        // Feed domain
        topics.put(FEED_POST_CREATED, TopicConfig.of(3, 7, KeyStrategy.COMMUNITY_ID));
        topics.put(FEED_REVIEW_SUBMITTED, TopicConfig.of(6, 30, KeyStrategy.PRODUCT_ID));

        // Platform domain
        topics.put(PLATFORM_EQUILIBRIUM_SIGNAL, TopicConfig.of(3, 7, KeyStrategy.SELLER_ID));
        topics.put(PLATFORM_FAILED_EVENTS, TopicConfig.of(1, 90, KeyStrategy.NONE));
        topics.put(PLATFORM_DATA_GAPS, TopicConfig.of(1, 90, KeyStrategy.NONE));
        topics.put(PLATFORM_DB_REPLICATION, TopicConfig.of(1, 3, KeyStrategy.NONE));
        topics.put(PLATFORM_SPACE_EVENTS, TopicConfig.of(3, 7, KeyStrategy.NONE));

        // Finance domain
        topics.put(FINANCE_INVOICE_EVENTS, TopicConfig.of(3, 30, KeyStrategy.SELLER_ID));
        topics.put(FINANCE_CONTRACT_EVENTS, TopicConfig.of(3, 30, KeyStrategy.SELLER_ID));

        // Hub operations (cross-dock coordination)
        topics.put(HUB_INBOUND_ARRIVED, TopicConfig.of(3, 7, KeyStrategy.NONE));
        topics.put(HUB_OUTBOUND_READY, TopicConfig.of(3, 7, KeyStrategy.NONE));
        topics.put(HUB_PROCESSING_DELAYED, TopicConfig.of(1, 30, KeyStrategy.NONE));

        // Order/Demand domain
        topics.put(ORDER_CREATED, TopicConfig.of(6, 14, KeyStrategy.ORDER_ID));
        topics.put(ORDER_UPDATED, TopicConfig.of(6, 14, KeyStrategy.ORDER_ID));
        topics.put(ORDER_ALLOCATED, TopicConfig.of(6, 7, KeyStrategy.ORDER_ID));
        topics.put(ORDER_FULFILLED, TopicConfig.of(6, 14, KeyStrategy.ORDER_ID));
        topics.put(FULFILLMENT_ORDER_CREATED, TopicConfig.of(6, 14, KeyStrategy.ORDER_ID));
        topics.put(REPLENISHMENT_ORDER_CREATED, TopicConfig.of(3, 14, KeyStrategy.PRODUCT_ID));

        return Collections.unmodifiableMap(topics);
    }
}
