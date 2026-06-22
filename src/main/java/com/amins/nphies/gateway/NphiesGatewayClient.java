package com.amins.nphies.gateway;

import com.amins.nphies.auth.NphiesTokenStore;
import com.amins.nphies.exception.NphiesAuthException;
import com.amins.nphies.exception.NphiesCircuitOpenException;
import com.amins.nphies.exception.NphiesException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Central HTTP client for all NPHIES API interactions.
 *
 * Wraps every call with a per-tenant Resilience4j stack:
 *   RateLimiter → CircuitBreaker → Retry
 *
 * Token injection: Bearer token auto-attached from NphiesTokenStore.
 * 401 handling: Token invalidated and request retried ONCE automatically.
 */
@Component
@Slf4j
public class NphiesGatewayClient {

    private final WebClient              nphiesWebClient;
    private final NphiesTokenStore       tokenStore;
    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry          retryRegistry;
    private final RateLimiterRegistry    rateLimiterRegistry;

    public NphiesGatewayClient(WebClient nphiesWebClient,
                               NphiesTokenStore tokenStore,
                               CircuitBreakerRegistry cbRegistry,
                               RetryRegistry retryRegistry,
                               RateLimiterRegistry rateLimiterRegistry) {
        this.nphiesWebClient     = nphiesWebClient;
        this.tokenStore          = tokenStore;
        this.cbRegistry          = cbRegistry;
        this.retryRegistry       = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    /**
     * Submits a FHIR Bundle to NPHIES.
     * Used for all transaction types: eligibility, preauth, claim, communication.
     *
     * @param tenantId     the submitting tenant
     * @param apiBaseUrl   tenant's NPHIES base URL (from TenantNphiesConfig)
     * @param bundleJson   serialized FHIR Bundle JSON string
     * @return NPHIES response body as String (parse with HAPI FHIR in service layer)
     */
    public String submitBundle(String tenantId, String apiBaseUrl, String bundleJson) {
        log.info("Submitting FHIR Bundle to NPHIES for tenant: {}", tenantId);

        return executeWithResilience(tenantId, () ->
                executePost(tenantId, apiBaseUrl + NphiesEndpoints.BUNDLE_ENDPOINT, bundleJson)
        );
    }

    /**
     * Polls NPHIES for a response to a previously submitted Bundle.
     * Called by the polling scheduler on pending transactions.
     *
     * @param tenantId    the tenant to poll for
     * @param apiBaseUrl  tenant's NPHIES base URL
     * @param bundleId    the NPHIES-assigned bundle UUID to poll
     * @return response body, or null if not yet available
     */
    public String pollBundleResponse(String tenantId, String apiBaseUrl, String bundleId) {
        log.debug("Polling NPHIES bundle response for tenant: {} bundleId: {}", tenantId, bundleId);

        String url = apiBaseUrl + NphiesEndpoints.BUNDLE_ENDPOINT + "/" + bundleId;
        return executeWithResilience(tenantId, () -> executeGet(tenantId, url));
    }

    // ── Core HTTP Execution ──────────────────────────────────────────────────

    private String executePost(String tenantId, String url, String body) {
        String token = tokenStore.getValidToken(tenantId);

        try {
            return nphiesWebClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.CONTENT_TYPE, "application/fhir+json")
                    .header(HttpHeaders.ACCEPT, "application/fhir+json")
                    .header("X-Tenant-Id", tenantId) // for NPHIES audit / internal tracing
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> status == HttpStatus.UNAUTHORIZED,
                            response -> {
                                log.warn("NPHIES returned 401 for tenant: {} — invalidating token", tenantId);
                                tokenStore.invalidateToken(tenantId);
                                return Mono.error(new NphiesAuthException(
                                        "NPHIES 401 Unauthorized — token invalidated for tenant: " + tenantId));
                            }
                    )
                    .onStatus(
                            status -> status.is4xxClientError() && status != HttpStatus.UNAUTHORIZED,
                            response -> response.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("NPHIES 4xx error for tenant: {}. Status: {}. Body: {}",
                                        tenantId, response.statusCode(), errBody);
                                // 4xx = don't retry (bad request/validation) — throw non-retryable
                                return Mono.error(new NphiesException.NonRetryable(
                                        "NPHIES rejected request: " + response.statusCode() + " — " + errBody));
                            })
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> {
                                log.warn("NPHIES 5xx error for tenant: {} — will retry", tenantId);
                                // 5xx = retryable
                                return Mono.error(new NphiesException.Retryable(
                                        "NPHIES server error for tenant: " + tenantId));
                            }
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

        } catch (WebClientResponseException e) {
            throw new NphiesException("NPHIES HTTP error: " + e.getStatusCode(), e);
        }
    }

    private String executeGet(String tenantId, String url) {
        String token = tokenStore.getValidToken(tenantId);

        try {
            return nphiesWebClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/fhir+json")
                    .retrieve()
                    .onStatus(
                            status -> status == HttpStatus.UNAUTHORIZED,
                            response -> {
                                tokenStore.invalidateToken(tenantId);
                                return Mono.error(new NphiesAuthException(
                                        "NPHIES 401 on poll for tenant: " + tenantId));
                            }
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .defaultIfEmpty("") // not-ready response
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.debug("NPHIES response not yet available for tenant: {}", tenantId);
            return "";
        }
    }

    // ── Resilience4j Pipeline ────────────────────────────────────────────────

    /**
     * Wraps a NPHIES call with per-tenant: RateLimiter → CircuitBreaker → Retry.
     *
     * Per-tenant isolation means one tenant's NPHIES failures/rate limits
     * do NOT affect other tenants.
     */
    private String executeWithResilience(String tenantId, java.util.concurrent.Callable<String> call) {
        // Get or create per-tenant resilience instances
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("nphies-" + tenantId);
        CircuitBreaker cb       = cbRegistry.circuitBreaker("nphies-" + tenantId);
        Retry retry             = retryRegistry.retry("nphies-" + tenantId);

        // Compose: RateLimiter → CircuitBreaker → Retry → actual call
        java.util.concurrent.Callable<String> rateLimited  = RateLimiter.decorateCallable(rateLimiter, call);
        java.util.concurrent.Callable<String> withCb       = CircuitBreaker.decorateCallable(cb, rateLimited);
        java.util.concurrent.Callable<String> withRetry    = Retry.decorateCallable(retry, withCb);

        try {
            return withRetry.call();
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            log.error("NPHIES circuit breaker OPEN for tenant: {}. Submissions paused.", tenantId);
            throw new NphiesCircuitOpenException(
                    "NPHIES circuit breaker is OPEN for tenant: " + tenantId +
                            ". Request queued for retry when circuit closes.", e);
        } catch (NphiesException.NonRetryable e) {
            throw e; // Propagate as-is — validation/4xx errors not retried
        } catch (NphiesAuthException e) {
            throw e; // Auth failures must not be re-wrapped — GlobalExceptionHandler handles them
        } catch (Exception e) {
            throw new NphiesException("NPHIES call failed for tenant: " + tenantId, e);
        }
    }
}
