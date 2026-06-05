package com.bhagwat.scm.core.rest.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class ApiClient extends AbstractApiClient {
    private  final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    public ApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Basic variant (original).
     *
     * @param apiConfig
     * @param responseType
     */
    @Override
    public <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Class<T> responseType) {
        return execute(apiConfig, responseType).block();
    }

    @Override
    public <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Object body, Class<T> responseType) {
        return execute(apiConfig, body, responseType).block();
    }

    @Override
    public <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Map<String, String> pathVariables, Class<T> responseType) {
        return executeWithWebClient(apiConfig, pathVariables, null, null, null, responseType).block();
    }

    @Override
    public <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Map<String, String> pathVariables, Map<String, String> queryParams, Object body, Class<T> responseType) {
        return executeWithWebClient(apiConfig, pathVariables, queryParams, body, null, responseType).block();
    }

    @Override
    public <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Map<String, String> pathVariables, Map<String, String> queryParams, Object body, Map<String, String> headers, Class<T> responseType) {
        return executeWithWebClient(apiConfig, pathVariables, queryParams, body, headers, responseType).block();
    }


    public <T> Mono<ResponseEntity<T>> execute(ApiConfig apiConfig, Class<T> responseType) {
        return executeWithWebClient(apiConfig, null, null, null, null, responseType);
    }

    public <T> Mono<ResponseEntity<T>> execute(ApiConfig apiConfig, Object body, Class<T> responseType) {
        return executeWithWebClient(apiConfig, null, null, body, null, responseType);
    }

    private <T> Mono<ResponseEntity<T>> executeWithWebClient(ApiConfig apiConfig, Map<String, String> pathVariable, Map<String, String> queryParams, Object body, Map<String, String> headers, Class<T> responseType) {
        return prepareRequest(apiConfig, pathVariable, queryParams, body, headers)
                .retrieve()
                .toEntity(responseType);
    }
    private WebClient.RequestBodySpec prepareRequest(ApiConfig apiConfig, Map<String, String> pathVariable, Map<String, String> queryParams, Object body, Map<String, String> headers) {
        checkNotNull(apiConfig);
        log.debug("prepare request");
        String fullUrl = getFullUrl(apiConfig, pathVariable, queryParams);
        log.debug("calling full url"+fullUrl);
        Map<String, String> allHeaders = mergeWithDefaultHeaders(headers, body != null);
        log.debug("allheaders"+allHeaders);
        if(body!= null)
            log.debug("body is"+body);
        WebClient.RequestBodySpec requestBodySpec = mutateWebClientIfRequired(webClient, apiConfig)
                .method(apiConfig.getHttpMethod())
                .uri(fullUrl)
                .headers(httpHeaders -> allHeaders.forEach(httpHeaders::add));
        if (body != null) {
            requestBodySpec.bodyValue(body);
        }
        return requestBodySpec;
    }


}
