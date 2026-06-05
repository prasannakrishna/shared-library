package com.bhagwat.scm.config;

import com.bhagwat.scm.filter.TenantInterceptor;
import com.bhagwat.scm.multitenancy.TenantSchemaProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

@Slf4j
@AutoConfiguration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, MongoAutoConfiguration.class})
@AutoConfigureBefore(MongoDataAutoConfiguration.class)
@EnableConfigurationProperties(MultiTenancyProperties.class)
@ConditionalOnProperty(prefix = "multitenancy", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(MongoTenantConfiguration.class)
public class MultiTenancyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantInterceptor.class)
    public TenantInterceptor tenantInterceptor(MultiTenancyProperties properties) {
        return new TenantInterceptor(properties);
    }

    @Bean
    public WebMvcConfigurer tenantMvcConfigurer(TenantInterceptor tenantInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                log.info("Registering TenantInterceptor");
                registry.addInterceptor(tenantInterceptor)
                        .excludePathPatterns("/actuator/**");
            }
        };
    }

    @Bean
    @Primary
    public DataSource tenantAwareDataSource(@Qualifier("dataSource") DataSource dataSource,
                                            MultiTenancyProperties properties) {
        log.info("Creating TenantAwareDataSource");
        return new TenantAwareDataSource(dataSource, properties);
    }

    /**
     * Exposes TenantSchemaProvisioner so services can inject it into their
     * startup initializers and Kafka listeners without extra config.
     *
     * Uses the raw (pre-routing) DataSource so provisioning always operates
     * against the real connection, not the tenant-scoped wrapper.
     */
    @Bean
    @ConditionalOnMissingBean(TenantSchemaProvisioner.class)
    public TenantSchemaProvisioner tenantSchemaProvisioner(
            @Qualifier("dataSource") DataSource rawDataSource,
            MultiTenancyProperties properties,
            Environment env) {

        String serviceName = (properties.getProvisioning().getServiceName() != null
                && !properties.getProvisioning().getServiceName().isBlank())
                ? properties.getProvisioning().getServiceName()
                : env.getProperty("spring.application.name", "unknown-service");

        log.info("Creating TenantSchemaProvisioner for service '{}' with migrations at '{}'",
                serviceName, properties.getProvisioning().getMigrationsLocation());

        return new TenantSchemaProvisioner(
                rawDataSource,
                serviceName,
                properties.getProvisioning().getMigrationsLocation());
    }
}
