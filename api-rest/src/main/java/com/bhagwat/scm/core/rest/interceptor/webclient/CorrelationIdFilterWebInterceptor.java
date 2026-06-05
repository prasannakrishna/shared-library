package com.bhagwat.scm.core.rest.interceptor.webclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class CorrelationIdFilterWebInterceptor implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(AuthTokenPropagationWebInterceptor.class);
    private final String correlationIdMdcValue;
    private final String outGoingCorrelationIdHeader;

    public CorrelationIdFilterWebInterceptor(String correlationIdMdcValue, String outGoingCorrelationIdHeader) {
        this.correlationIdMdcValue = correlationIdMdcValue;
        this.outGoingCorrelationIdHeader = outGoingCorrelationIdHeader;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction nextFunction) {

        String correlationId = Optional.ofNullable(correlationIdMdcValue)
                .map(MDC::get)
                .filter(id -> !id.isBlank())
                .orElse(null);
        if (correlationId == null) {
            return nextFunction.exchange(clientRequest);
        }
        log.debug("sending correlationId in api header {} : {}", outGoingCorrelationIdHeader, correlationId);
        return nextFunction.exchange(ClientRequest.from(clientRequest).header(outGoingCorrelationIdHeader, correlationId)
                .build());

    }
}
