package com.bhagwat.scm.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoTenantConfiguration {

    private final MultiTenancyProperties multiTenancyProperties;
    private final MongoProperties mongoProperties;

    // Pattern to extract database name from MongoDB URI
    // mongodb://localhost:27017/inventorydb
    private static final Pattern DB_PATTERN = Pattern.compile("mongodb://[^/]+/([^?]+)");

    @Bean
    @ConditionalOnMissingBean(MongoDatabaseFactory.class)
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return new TenantAwareMongoDatabaseFactory(
                mongoProperties.getUri(),
                multiTenancyProperties
        );
    }

    @Bean
    @ConditionalOnMissingBean(MongoTemplate.class)
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    /**
     * Tenant-aware MongoDB Database Factory
     * Resolves database name as: {tenantId}_{baseDatabase}
     */
    public static class TenantAwareMongoDatabaseFactory extends SimpleMongoClientDatabaseFactory {

        private final String baseDatabase;
        private final MultiTenancyProperties properties;

        public TenantAwareMongoDatabaseFactory(String uri, MultiTenancyProperties properties) {
            super(createMongoClient(uri), extractBaseDatabase(uri));
            this.baseDatabase = extractBaseDatabase(uri);
            this.properties = properties;
            log.info("TenantAwareMongoDatabaseFactory initialized with base database: {}", baseDatabase);
        }

        @Override
        protected String getDefaultDatabaseName() {
            String tenantId = com.bhagwat.scm.config.TenantContext.getTenantId();

            if (tenantId == null) {
                log.debug("No tenant context, using base database: {}", baseDatabase);
                return baseDatabase;
            }

            // Build tenant-specific database name: {tenantId}_{baseDatabase}
            String tenantDatabase = tenantId + properties.getSchemaSeparator() + baseDatabase;
            log.info("Resolved MongoDB database for tenant {}: {}", tenantId, tenantDatabase);

            return tenantDatabase;
        }

        /**
         * Extract base database name from MongoDB URI
         */
        private static String extractBaseDatabase(String uri) {
            Matcher matcher = DB_PATTERN.matcher(uri);
            if (matcher.find()) {
                String database = matcher.group(1);
                log.info("Extracted base database from MongoDB URI: {}", database);
                return database;
            }

            log.warn("Could not extract database from MongoDB URI: {}", uri);
            return "test"; // Default MongoDB database
        }

        /**
         * Create MongoClient from URI
         */
        private static MongoClient createMongoClient(String uri) {
            ConnectionString connectionString = new ConnectionString(uri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();
            return MongoClients.create(settings);
        }
    }
}