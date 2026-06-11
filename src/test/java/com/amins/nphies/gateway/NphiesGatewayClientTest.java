package com.amins.nphies.gateway;

import com.amins.nphies.auth.NphiesTokenStore;
import com.amins.nphies.exception.NphiesCircuitOpenException;
import com.amins.nphies.exception.NphiesException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NphiesGatewayClientTest {

    @Mock NphiesTokenStore tokenStore;

    private MockWebServer mockWebServer;
    private NphiesGatewayClient gatewayClient;

    private static final String TENANT    = "hospital-001";
    private static final String BUNDLE_ID = "bundle-uuid-123";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        // Lenient retry/rate-limiter/circuit-breaker for unit tests
        RetryConfig noRetry = RetryConfig.custom()
                .maxAttempts(1)
                .build();
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(100)
                .failureRateThreshold(100)
                .build();
        RateLimiterConfig rlConfig = RateLimiterConfig.custom()
                .limitForPeriod(Integer.MAX_VALUE)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(1))
                .build();

        gatewayClient = new NphiesGatewayClient(
                webClient,
                tokenStore,
                CircuitBreakerRegistry.of(cbConfig),
                RetryRegistry.of(noRetry),
                RateLimiterRegistry.of(rlConfig)
        );

        when(tokenStore.getValidToken(TENANT)).thenReturn("test-bearer-token");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ── submitBundle ─────────────────────────────────────────────────────────

    @Test
    void submitBundle_success_returnsResponseBody() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/fhir+json")
                .setBody("{\"resourceType\":\"Bundle\"}"));

        String result = gatewayClient.submitBundle(
                TENANT, mockWebServer.url("/r4").toString(), "{\"resourceType\":\"Bundle\"}");

        assertThat(result).isEqualTo("{\"resourceType\":\"Bundle\"}");
    }

    @Test
    void submitBundle_sendsAuthorizationHeader() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        gatewayClient.submitBundle(TENANT, mockWebServer.url("/r4").toString(), "{}");

        var request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-bearer-token");
    }

    @Test
    void submitBundle_sendsFhirContentTypeHeader() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        gatewayClient.submitBundle(TENANT, mockWebServer.url("/r4").toString(), "{}");

        var request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Content-Type")).contains("application/fhir+json");
    }

    @Test
    void submitBundle_401_invalidatesTokenAndThrows() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() ->
                gatewayClient.submitBundle(TENANT, mockWebServer.url("/r4").toString(), "{}"))
                .isInstanceOf(Exception.class);

        verify(tokenStore).invalidateToken(TENANT);
    }

    @Test
    void submitBundle_4xx_throwsNonRetryable() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(422)
                .setBody("Validation failed"));

        assertThatThrownBy(() ->
                gatewayClient.submitBundle(TENANT, mockWebServer.url("/r4").toString(), "{}"))
                .isInstanceOf(NphiesException.NonRetryable.class);
    }

    @Test
    void submitBundle_5xx_throwsNphiesException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() ->
                gatewayClient.submitBundle(TENANT, mockWebServer.url("/r4").toString(), "{}"))
                .isInstanceOf(NphiesException.class);
    }

    // ── pollBundleResponse ───────────────────────────────────────────────────

    @Test
    void pollBundleResponse_success_returnsBody() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"resourceType\":\"ClaimResponse\"}"));

        String result = gatewayClient.pollBundleResponse(
                TENANT, mockWebServer.url("/r4").toString(), BUNDLE_ID);

        assertThat(result).isEqualTo("{\"resourceType\":\"ClaimResponse\"}");
    }

    @Test
    void pollBundleResponse_404_returnsEmptyString() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        String result = gatewayClient.pollBundleResponse(
                TENANT, mockWebServer.url("/r4").toString(), BUNDLE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void pollBundleResponse_401_invalidatesToken() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() ->
                gatewayClient.pollBundleResponse(
                        TENANT, mockWebServer.url("/r4").toString(), BUNDLE_ID))
                .isInstanceOf(Exception.class);

        verify(tokenStore).invalidateToken(TENANT);
    }

    @Test
    void pollBundleResponse_sendsCorrectUrl() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        gatewayClient.pollBundleResponse(
                TENANT, mockWebServer.url("/r4").toString(), BUNDLE_ID);

        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).endsWith("/r4/Bundle/" + BUNDLE_ID);
    }
}
