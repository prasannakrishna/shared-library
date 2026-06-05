package com.bhagwat.scm.core.rest.config;

import com.bhagwat.scm.core.rest.api.ApiClient;
import com.bhagwat.scm.core.rest.interceptor.webclient.AuthTokenPropagationWebInterceptor;
import com.bhagwat.scm.core.rest.interceptor.webclient.CorrelationIdFilterWebInterceptor;
import com.bhagwat.scm.core.rest.interceptor.webclient.WebClientLoggingWebInterceptor;
import com.bhagwat.scm.core.rest.util.ApiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({RestClientProperties.class, ServiceApiProperties.class})
public class RestClientConfig {

    @Autowired
    private RestClientProperties props;

    @Bean
    @ConditionalOnMissingBean
    public WebClient webClient() {
        WebClient.Builder builder = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(props.getMaxBufferSize()))
                .clientConnector(new ReactorClientHttpConnector(
                        ApiUtils.getHttpClient(
                                props.getConnectionTimeOut(),
                                props.getRequestTimeOut(),
                                props.getResponseTimeOut())))
                // Always forward Bearer token to downstream service via Tyk
                .filter(new AuthTokenPropagationWebInterceptor())
                // Always forward correlation ID for distributed tracing
                .filter(new CorrelationIdFilterWebInterceptor(
                        props.getCorrelationId().getCoreMdcKey(),
                        props.getCorrelationId().getOutgoingHeader()));

        // Optionally add Tyk API key header (for services that require a Tyk API key
        // in addition to the Bearer token — configured per environment)
        if (props.getTyk().isHeaderKeyEnabled()) {
            builder.filter(tykApiKeyFilter(props.getTyk()));
        }

        // Optionally enable request/response logging
        if (props.getLogging().isEnabled()) {
            builder.filter(new WebClientLoggingWebInterceptor(props.getLogging()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient apiClient(WebClient webClient) {
        return new ApiClient(webClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceApiRegistry serviceApiRegistry(ServiceApiProperties serviceApiProperties) {
        return new ServiceApiRegistry(serviceApiProperties);
    }

    private ExchangeFilterFunction tykApiKeyFilter(RestClientProperties.Tyk tyk) {
        return (request, next) -> next.exchange(
                ClientRequest.from(request)
                        .headers(h -> h.set(tyk.getHeaderKey(), tyk.getHeaderValue()))
                        .build());
    }
}
