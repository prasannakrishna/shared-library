package com.bhagwat.scm.core.rest.interceptor.webclient;

import com.bhagwat.scm.core.rest.config.RestClientProperties;
import com.bhagwat.scm.core.rest.util.PathFilterUtil;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebClientLoggingWebInterceptor implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(WebClientLoggingWebInterceptor.class);
    private final RestClientProperties.Logging loggingProperties;

    public WebClientLoggingWebInterceptor(RestClientProperties.Logging loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (!loggingProperties.isEnabled()) {
            log.debug("webclient logging filter is disabled");
        }
        String path = request.url().getPath();
        if (PathFilterUtil.shouldSkipLogging(path, loggingProperties.getIncludePaths(), loggingProperties.getExcludePaths())) {
            log.debug("skip logging for path {}", path);
            return next.exchange(request);
        }
        logBasicRequest(request);
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        ClientRequest clientRequest = loggingProperties.getRequestBody().isEnabled() ? logRequestBody(request, mdcContext) : request;

        return next.exchange(clientRequest)
                .flatMap(response -> {
                    logBasicResponse(response, mdcContext);
                    if (loggingProperties.getResponseBody().isEnabled()) {
                        return logResponseBody(response, mdcContext);
                    }
                    return Mono.just(response);
                });
    }

    private void logBasicRequest(ClientRequest request) {
        if (!loggingProperties.isEnabled() || !loggingProperties.getRequestBody().isEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n====== [WebClient Request] ======\n")
                .append("➡️  Method  : ").append(request.method()).append("\n")
                .append("➡️  URL     : ").append(request.url()).append("\n");

        if (loggingProperties.getRequestBody().isEnabled()) {
            sb.append("➡️  Headers : ").append(request.headers()).append("\n");
        }

        log.info(sb.toString());
    }

    private ClientRequest logRequestBody(ClientRequest request, Map<String, String> mdcContext) {
        int max = loggingProperties.getRequestBody().getMaxSize();
        if (!loggingProperties.isEnabled() || !loggingProperties.getRequestBody().isEnabled()) {
            return request;
        }
        return ClientRequest.from(request)
                .body((outputMessage, context) -> {
                    ClientHttpRequestDecorator loggingRequest =
                            new ClientHttpRequestDecorator(outputMessage) {
                                @Override
                                public @NonNull Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                                    return super.writeWith(
                                            Flux.from(body)
                                                    .doOnNext(buffer -> {
                                                        MDC.setContextMap(mdcContext);
                                                        try {
                                                            log.info("request body: {}", buffer.toString(0, max, StandardCharsets.UTF_8));
                                                        } catch (Exception e) {
                                                            log.error("error logging request" + e.getMessage());
                                                        }
                                                    })
                                    );
                                }
                            };
                    return request.body().insert(loggingRequest, context);
                })
                .build();
    }

    private void logBasicResponse(ClientResponse response, Map<String, String> mdcContext) {
        if (!loggingProperties.isEnabled() || !loggingProperties.getResponseBody().isEnabled()) {
            return;
        }

        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n====== [WebClient Response] ======\n")
                .append("⬅️  Status  : ").append(response.statusCode()).append("\n");

        if (loggingProperties.getResponseBody().isEnabled()) {
            sb.append("⬅️  Headers : ").append(response.headers().asHttpHeaders()).append("\n");
        }

        log.info(sb.toString());
    }

    /**
     * Logs the response body (if enabled). We buffer it using bodyToMono(String.class)
     * and then recreate a new ClientResponse so the downstream consumer can still read it.
     */
    private Mono<ClientResponse> logResponseBody(ClientResponse response, Map<String, String> mdcContext) {
        int max = loggingProperties.getRequestBody().getMaxSize();
        if (!loggingProperties.isEnabled() || !loggingProperties.getResponseBody().isEnabled()) {
            return Mono.just(response);
        }

        return response.bodyToMono(byte[].class)
                .defaultIfEmpty(new byte[0])
                .flatMap(bodyBytes -> {
                    try {
                        MDC.setContextMap(mdcContext);
                        String body = new String(bodyBytes, StandardCharsets.UTF_8);
                        if (bodyBytes.length > max) {
                            body = body.substring(0, max);
                        }
                        log.info("response body {}", body);

                    } catch (Exception e) {
                        log.error("error logging response body {}", e.getMessage());
                    }

                    // recreate response so the chain can still consume it
                    return Mono.just(ClientResponse.create(response.statusCode())
                            .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                            .body(Flux.just(new DefaultDataBufferFactory().wrap(bodyBytes)))
                            .build());
                });
    }
}
