package com.bhagwat.scm.core.rest.util;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ApiUtils {
    public static HttpClient getHttpClient(int connectionTimeout, int requestTimeout, int responseTimeout) {
    return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
            .responseTimeout(Duration.ofMillis(requestTimeout))
            .doOnConnected(conn ->
                    conn.addHandlerFirst(new ReadTimeoutHandler(requestTimeout, TimeUnit.MILLISECONDS)
                    ).addHandlerLast(new WriteTimeoutHandler(requestTimeout, TimeUnit.MILLISECONDS)));
    }
}
