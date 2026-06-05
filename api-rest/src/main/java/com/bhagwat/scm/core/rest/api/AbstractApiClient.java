package com.bhagwat.scm.core.rest.api;

import com.bhagwat.scm.core.rest.util.ApiUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractApiClient {
    /**
     * Basic variant (original).
     */
    public abstract <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Class<T> responseType);

    public abstract <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Object body, Class<T> responseType);

    public abstract <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Map<String, String> pathVariables, Class<T> responseType);

    public abstract <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Map<String, String> pathVariables,
                                                 Map<String, String> queryParams, Object body, Class<T> responseType);

    public abstract <T> ResponseEntity<T> invoke(ApiConfig apiConfig, Map<String, String> pathVariables, Map<String, String> queryParams,
           Object body, Map<String, String> headers, Class<T> responseType
    );



    /**
     * Helper to validate ApiConfig before executing a call.
     * Throws IllegalArgumentException if required fields are missing.
     *
     * @param apiConfig api config to validate
     */
    protected void checkNotNull(ApiConfig apiConfig) {
        if (apiConfig == null) {
            throw new IllegalArgumentException("ApiConfig must not be null");
        }
        if (isNullOrEmpty(apiConfig.getHost())) {
            throw new IllegalArgumentException("ApiConfig.host must not be null or empty");
        }
        if (isNullOrEmpty(apiConfig.getApiPath())) {
            // depending on your semantics, apiPath could be optional — adjust as needed
            throw new IllegalArgumentException("ApiConfig.apiPath must not be null or empty");
        }
        if (apiConfig.getHttpMethod() == null) {
            throw new IllegalArgumentException("ApiConfig.httpMethod must not be null");
        }
        // Optional: validate timeouts if needed (non-negative)
        if (apiConfig.getConnectionTimeout() < 0 ||
                apiConfig.getRequestTimeout() < 0 ||
                apiConfig.getResponseTimeout() < 0) {
            throw new IllegalArgumentException("Timeout values in ApiConfig must not be negative");
        }
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }


    protected String getFullUrl(ApiConfig apiConfig, Map<String, String> pathVariables, Map<String, String> queryParams) {
        String url = apiConfig.getHost() + apiConfig.getApiPath();
        UriComponentsBuilder uribuilder = UriComponentsBuilder.fromUriString(url);

        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach(uribuilder::queryParam);
        }

        return uribuilder.buildAndExpand(pathVariables!= null ? pathVariables:Map.of()).toUriString();
    }

    protected WebClient mutateWebClientIfRequired(WebClient webClient, ApiConfig apiConfig) {
        if (!apiConfig.isOverrideDefaultHttpProperties()) {
            return webClient;
        }

        return webClient.mutate()
                .clientConnector(new ReactorClientHttpConnector(
                        ApiUtils.getHttpClient(apiConfig.getConnectionTimeout(),
                                apiConfig.getRequestTimeout(),
                                apiConfig.getResponseTimeout())
                ))
                .build();
    }

    protected Map<String, String> mergeWithDefaultHeaders(Map<String, String> headers, boolean hasBody) {
        Map<String, String> result = headers== null ? new HashMap<>() : new HashMap<>(headers);
        if(hasBody){
            result.putIfAbsent(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        result.putIfAbsent(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return result;
    }

    protected Map<String, String> toStringMap(Map<String, ?> map) {
        if (map == null) return Collections.emptyMap();
        Map<String, String> strMap = new HashMap<>();
        map.forEach((k, v) -> strMap.put(k, v != null ? v.toString() : ""));
        return strMap;
    }
}
