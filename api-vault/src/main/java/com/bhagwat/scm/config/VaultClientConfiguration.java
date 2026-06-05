package com.bhagwat.scm.config;

import com.bhagwat.scm.api.VaultClient;
import com.bhagwat.scm.api.VaultClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;


@Configuration
@ConditionalOnClass(VaultTemplate.class)
@EnableConfigurationProperties(VaultProperties.class)
public class VaultClientConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper vaultObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultEndpoint vaultEndpoint(VaultProperties props) {

        return VaultEndpoint.from(java.net.URI.create(props.getUri()));
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultTemplate vaultTemplate(
            VaultEndpoint endpoint,
            VaultProperties props) {

        return new VaultTemplate(endpoint,
                new TokenAuthentication(props.getToken()));
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultClient vaultClient(
            VaultTemplate vaultTemplate,
            ObjectMapper objectMapper,
            VaultProperties vaultProperties) {

        return new VaultClientImpl(vaultTemplate, objectMapper, vaultProperties);
    }
}
