package com.bhagwat.scm.kafka.registry;

/**
 * Standardized Consumer Group IDs.
 *
 * <h2>Naming Convention</h2>
 * <pre>
 *   {service-name}.{purpose}
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>One consumer group per logical consumer — if a service has 3 independent listeners
 *       processing different topics for different purposes, each gets its own group.</li>
 *   <li>Multiple instances of the same service SHARE the same group ID (this is how
 *       Kafka distributes partitions across instances for horizontal scaling).</li>
 *   <li>If two services consume the same topic for DIFFERENT purposes,
 *       they MUST have different group IDs (both get all messages).</li>
 * </ul>
 *
 * <h2>Scaling Impact</h2>
 * <pre>
 *   Topic: inventory.product.created (6 partitions)
 *   Consumer group: catalog-service.product-sync (3 instances)
 *   → Each instance gets 2 partitions (6/3 = 2)
 *   → Scale to 6 instances → each gets 1 partition (max parallelism)
 *   → Scale to 7+ instances → 1 instance sits idle (no more partitions to assign)
 * </pre>
 */
public final class ConsumerGroups {

    private ConsumerGroups() {}

    // ── catalogService consumers ─────────────────────────────────────────────

    /** Syncs product catalog entries from inventory events */
    public static final String CATALOG_PRODUCT_SYNC = "catalog-service.product-sync";

    /** Updates subscription snapshots (capacity, frequency slots) */
    public static final String CATALOG_SUBSCRIPTION_SYNC = "catalog-service.subscription-sync";

    /** Updates seller reliability scores from work order completions */
    public static final String CATALOG_RELIABILITY = "catalog-service.reliability";

    /** Processes trial order delivery events */
    public static final String CATALOG_TRIAL_DELIVERY = "catalog-service.trial-delivery";

    /** Aggregates demand from subscription events */
    public static final String CATALOG_DEMAND = "catalog-service.demand-aggregation";

    /** Syncs community membership changes for personalized feed */
    public static final String CATALOG_COMMUNITY_MEMBERSHIP = "catalog-service.community-membership";

    /** Refreshes subscription capacity snapshots in Redis */
    public static final String CATALOG_SNAPSHOT_REFRESH = "catalog-service.snapshot-refresh";

    // ── inventoryService consumers ───────────────────────────────────────────

    /** Syncs product/SKU to MongoDB read model */
    public static final String INVENTORY_MONGO_SYNC = "inventory-service.mongo-sync";

    /** Syncs inventory from store/warehouse events to central ledger */
    public static final String INVENTORY_CENTRAL_SYNC = "inventory-service.central-sync";

    /** Processes allocation events from orderGroupingService */
    public static final String INVENTORY_ALLOCATION = "inventory-service.allocation";

    /** Processes seller inventory publish events */
    public static final String INVENTORY_SELLER_SYNC = "inventory-service.seller-sync";

    /** Processes SKU sync events from sellerService */
    public static final String INVENTORY_SKU_SYNC = "inventory-service.sku-sync";

    /** Processes workflow completion affecting inventory readiness */
    public static final String INVENTORY_WORKFLOW = "inventory-service.workflow";

    // ── sellerService consumers ──────────────────────────────────────────────

    /** Auto-publishes inventory on work order completion */
    public static final String SELLER_AUTO_PUBLISH = "seller-service.auto-publish";

    /** Syncs pack config mirrors from catalog events */
    public static final String SELLER_PACKCONFIG_SYNC = "seller-service.packconfig-sync";

    /** Tracks shipment milestones and delivery for seller dashboard */
    public static final String SELLER_SHIPMENT_TRACKING = "seller-service.shipment-tracking";

    /** Processes unit cost events */
    public static final String SELLER_UNIT_ECONOMICS = "seller-service.unit-economics";

    /** Local SKU → MongoDB sync */
    public static final String SELLER_SKU_LOCAL_SYNC = "seller-service.sku-local-sync";

    /** Local pack config → MongoDB sync */
    public static final String SELLER_PACKCONFIG_LOCAL_SYNC = "seller-service.packconfig-local-sync";

    /** Tenant provisioning on org creation */
    public static final String SELLER_TENANT_PROVISIONER = "seller-service.tenant-provisioner";

    // ── communityManager consumers ───────────────────────────────────────────

    /** Indexes new products in Elasticsearch for community discovery */
    public static final String COMMUNITY_PRODUCT_INDEXER = "community-manager.product-indexer";

    /** Computes product-community keyword affinity */
    public static final String COMMUNITY_AFFINITY_COMPUTE = "community-manager.affinity-compute";

    // ── feedService consumers ────────────────────────────────────────────────

    /** Creates community feed on community creation */
    public static final String FEED_COMMUNITY_SETUP = "feed-service.community-setup";

    /** Creates pending review slots on order delivery */
    public static final String FEED_PENDING_REVIEWS = "feed-service.pending-reviews";

    // ── orderGroupingService consumers ───────────────────────────────────────

    /** Processes cart checkout events into grouped orders */
    public static final String ORDER_GROUPING = "order-grouping-service.checkout";

    // ── transportPlanner consumers ───────────────────────────────────────────

    /** Plans transport routes from RTS events */
    public static final String TRANSPORT_PLANNING = "transport-planner.route-planning";

    /** Tracks shipment milestone updates */
    public static final String TRANSPORT_MILESTONE_TRACKING = "transport-planner.milestone";

    // ── Platform consumers ───────────────────────────────────────────────────

    /** Database replication */
    public static final String PLATFORM_DB_REPLICATION = "platform.db-replication";

    /** Space allocation */
    public static final String PLATFORM_SPACE = "space-service.events";

    // ── auditService consumers ───────────────────────────────────────────────

    /** Consumes failed events (DLT centralized audit) */
    public static final String AUDIT_FAILED_EVENTS = "audit-service.failed-events";

    /** Consumes analytics data gap events */
    public static final String AUDIT_DATA_GAPS = "audit-service.data-gaps";
}
