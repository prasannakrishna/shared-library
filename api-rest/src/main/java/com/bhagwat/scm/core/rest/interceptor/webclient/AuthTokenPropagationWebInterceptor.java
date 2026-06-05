package com.bhagwat.scm.core.rest.interceptor.webclient;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import static com.bhagwat.scm.core.rest.config.Constants.AUTHORIZATION;

public class AuthTokenPropagationWebInterceptor implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(AuthTokenPropagationWebInterceptor.class);

    @Override
    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction nextFilter) {
        // Read from the thread-local on the calling (servlet) thread before Netty takes over.
        // If the header was already set explicitly (e.g. via invoke headers map), skip.
        if (clientRequest.headers().containsKey(AUTHORIZATION)) {
            return nextFilter.exchange(clientRequest);
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(AUTHORIZATION);
            if (authHeader != null && !authHeader.isBlank()) {
                return nextFilter.exchange(ClientRequest.from(clientRequest)
                        .headers(h -> h.set(AUTHORIZATION, authHeader))
                        .build());
            }
        }
        log.warn("No Auth Header found to propagate");
        return nextFilter.exchange(clientRequest);
    }
}
