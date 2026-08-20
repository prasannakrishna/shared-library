package com.bhagwat.scm.kafka.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps legacy topic names to their standardized equivalents.
 *
 * <h2>Migration Strategy</h2>
 * <ol>
 *   <li>Deploy producer to write to BOTH old and new topic (dual-write phase)</li>
 *   <li>Deploy consumer to read from NEW topic</li>
 *   <li>Verify no lag on new topic</li>
 *   <li>Remove old topic from producer</li>
 *   <li>Delete old topic after retention expires</li>
 * </ol>
 *
 * <h2>Usage during migration</h2>
 * <pre>{@code
 *   // In service code:
 *   String topic = TopicMigrationMap.resolve("product-created-topic");
 *   // Returns "inventory.product.created" (the new name)
 *
 *   // Or use the constant directly:
 *   kafkaProducer.send(TopicRegistry.INVENTORY_PRODUCT_CREATED, key, event);
 * }</pre>
 */
public final class TopicMigrationMap {

    private TopicMigrationMap() {}

    private static final Map<String, String> LEGACY_TO_STANDARD;

    static {
        Map<String, String> map = new LinkedHashMap<>();

        // ── Inventory/Product ────────────────────────────────────────────
        map.put("product-created-topic", TopicRegistry.INVENTORY_PRODUCT_CREATED);
        map.put("product-updated-topic", TopicRegistry.INVENTORY_PRODUCT_UPDATED);
        map.put("product-deleted-topic", TopicRegistry.INVENTORY_PRODUCT_DELETED);
        map.put("sku-created-topic", TopicRegistry.INVENTORY_SKU_CREATED);
        map.put("sku-updated-topic", TopicRegistry.INVENTORY_SKU_UPDATED);
        map.put("sku-deleted-topic", TopicRegistry.INVENTORY_SKU_DELETED);
        map.put("inventory-created-topic", TopicRegistry.INVENTORY_STOCK_PUBLISHED);
        map.put("inventory-events", TopicRegistry.INVENTORY_STOCK_PUBLISHED);
        map.put("seller.inventory.sync", TopicRegistry.INVENTORY_SELLER_SYNC);
        map.put("workflow-events", TopicRegistry.INVENTORY_WORKFLOW_COMPLETED);

        // ── Catalog ──────────────────────────────────────────────────────
        map.put("catalog-events", TopicRegistry.CATALOG_PACKCONFIG_CREATED);

        // ── Order ────────────────────────────────────────────────────────
        map.put("community.cart.checkout", TopicRegistry.ORDER_CART_CHECKOUT);
        map.put("payment.success", TopicRegistry.ORDER_PAYMENT_SUCCESS);
        map.put("order.delivery.events", TopicRegistry.ORDER_DELIVERY_COMPLETED);

        // ── Transport ────────────────────────────────────────────────────
        map.put("transport.rts.created", TopicRegistry.TRANSPORT_RTS_CREATED);
        map.put("transport.shipment.created", TopicRegistry.TRANSPORT_SHIPMENT_CREATED);
        map.put("transport.shipment.milestone", TopicRegistry.TRANSPORT_SHIPMENT_MILESTONE);
        map.put("transport.shipment.delivered", TopicRegistry.TRANSPORT_SHIPMENT_DELIVERED);

        // ── Seller ───────────────────────────────────────────────────────
        map.put("seller.workorder.completed", TopicRegistry.SELLER_WORKORDER_COMPLETED);
        map.put("seller.sku.events", TopicRegistry.SELLER_SKU_LOCAL);
        map.put("seller.pack-config.local", TopicRegistry.SELLER_PACKCONFIG_LOCAL);
        map.put("product.unit.cost.update", TopicRegistry.SELLER_UNITCOST_UPDATED);
        map.put("org.events", TopicRegistry.SELLER_ORG_EVENTS);

        // ── Community ────────────────────────────────────────────────────
        map.put("community.created", TopicRegistry.COMMUNITY_CREATED);
        map.put("community-membership-events", TopicRegistry.COMMUNITY_MEMBERSHIP_CHANGED);

        // ── Subscription ─────────────────────────────────────────────────
        map.put("subscription-events", TopicRegistry.SUBSCRIPTION_ACTIVATED);
        map.put("subscription.events", TopicRegistry.SUBSCRIPTION_ACTIVATED);
        map.put("subscription.demand.events", TopicRegistry.SUBSCRIPTION_DEMAND_AGGREGATED);

        // ── Feed ─────────────────────────────────────────────────────────
        map.put("feed.post.created", TopicRegistry.FEED_POST_CREATED);
        map.put("product.review.submitted", TopicRegistry.FEED_REVIEW_SUBMITTED);

        // ── Platform ─────────────────────────────────────────────────────
        map.put("seller.equilibrium.signals", TopicRegistry.PLATFORM_EQUILIBRIUM_SIGNAL);
        map.put("platform.failed-events", TopicRegistry.PLATFORM_FAILED_EVENTS);
        map.put("db.replication.events", TopicRegistry.PLATFORM_DB_REPLICATION);
        map.put("space-events", TopicRegistry.PLATFORM_SPACE_EVENTS);

        LEGACY_TO_STANDARD = Collections.unmodifiableMap(map);
    }

    /**
     * Resolve a topic name — returns the standard name if the input is a legacy name,
     * or the input itself if it's already standard.
     */
    public static String resolve(String topicName) {
        return LEGACY_TO_STANDARD.getOrDefault(topicName, topicName);
    }

    /**
     * Get the full mapping (for documentation/audit purposes).
     */
    public static Map<String, String> getAll() {
        return LEGACY_TO_STANDARD;
    }

    /**
     * Check if a topic name is legacy (needs migration).
     */
    public static boolean isLegacy(String topicName) {
        return LEGACY_TO_STANDARD.containsKey(topicName);
    }
}
