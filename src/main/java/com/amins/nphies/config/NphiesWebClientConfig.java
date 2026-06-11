package com.amins.nphies.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebClient beans for NPHIES communication.
 *
 * Two beans:
 *  1. nphiesWebClient    — for FHIR API calls (has auth injected per-request)
 *  2. plainWebClient     — for OAuth2 token endpoint (no auth header)
 */
@Configuration
@Slf4j
public class NphiesWebClientConfig {

    /**
     * Main WebClient for NPHIES FHIR API.
     * No default auth headers — injected per-request in NphiesGatewayClient.
     */
    @Bean("nphiesWebClient")
    public WebClient nphiesWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
                .responseTimeout(Duration.ofSeconds(60))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                )
                // Enable connection pooling (default Reactor Netty pool)
                .keepAlive(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB for large FHIR bundles
                .filter(requestLoggingFilter())
                .filter(responseLoggingFilter())
                .build();
    }

    /**
     * Plain WebClient for token endpoint only.
     * Shorter timeouts — token fetch should be fast.
     */
    @Bean("plainWebClient")
    public WebClient plainWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(15));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // ── Logging Filters ──────────────────────────────────────────────────────

    private ExchangeFilterFunction requestLoggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("NPHIES → {} {} [headers: Content-Type={}, Accept={}]",
                    request.method(), request.url(),
                    request.headers().getFirst("Content-Type"),
                    request.headers().getFirst("Accept"));
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction responseLoggingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("NPHIES ← status: {} [Content-Type: {}]",
                    response.statusCode(),
                    response.headers().contentType().orElse(null));
            return Mono.just(response);
        });
    }
}
