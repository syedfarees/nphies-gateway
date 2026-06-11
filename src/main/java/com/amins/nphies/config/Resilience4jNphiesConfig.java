package com.amins.nphies.config;

import com.amins.nphies.exception.NphiesAuthException;
import com.amins.nphies.exception.NphiesException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for NPHIES calls.
 *
 * All instances use the DEFAULT config defined here as template.
 * Per-tenant instances are created dynamically from this template
 * in NphiesGatewayClient using the registry.
 *
 * Tuning guide:
 *  - slidingWindowSize: increase for high-traffic tenants
 *  - waitDurationInOpenState: increase if NPHIES has known maintenance windows
 *  - limitForPeriod: adjust based on NPHIES rate limits per your license
 */
@Configuration
public class Resilience4jNphiesConfig {

    // ── Circuit Breaker ──────────────────────────────────────────────────────
    @Bean
    public CircuitBreakerRegistry nphiesCircuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Open after 50% failures in last 10 calls (sliding window)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50f)
                // Slow calls (>45s) also count as failures
                .slowCallDurationThreshold(Duration.ofSeconds(45))
                .slowCallRateThreshold(80f)
                // When OPEN, wait 30s before moving to HALF_OPEN
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // In HALF_OPEN state, allow 3 test calls
                .permittedNumberOfCallsInHalfOpenState(3)
                // Automatically transition OPEN → HALF_OPEN after wait
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // Only record NphiesException.Retryable as failure (not validation errors)
                .recordExceptions(
                        NphiesException.Retryable.class,
                        NphiesAuthException.class,
                        java.net.ConnectException.class,
                        java.util.concurrent.TimeoutException.class
                )
                // Do NOT trip circuit on validation/4xx errors — those are caller problems
                .ignoreExceptions(NphiesException.NonRetryable.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    // ── Retry ────────────────────────────────────────────────────────────────
    @Bean
    public RetryRegistry nphiesRetryRegistry() {
        RetryConfig config = RetryConfig.custom()
                // Max 3 attempts (1 original + 2 retries)
                .maxAttempts(3)
                // Exponential backoff: 1s, 2s, 4s
                .intervalFunction(
                        io.github.resilience4j.core.IntervalFunction
                                .ofExponentialBackoff(Duration.ofSeconds(1), 2.0)
                )
                // Only retry on transient/server errors
                .retryExceptions(
                        NphiesException.Retryable.class,
                        java.net.ConnectException.class,
                        java.util.concurrent.TimeoutException.class
                )
                // Never retry on validation/auth errors
                .ignoreExceptions(
                        NphiesException.NonRetryable.class,
                        NphiesAuthException.class  // Auth errors handled separately (token refresh)
                )
                .build();

        return RetryRegistry.of(config);
    }

    // ── Rate Limiter ─────────────────────────────────────────────────────────
    @Bean
    public RateLimiterRegistry nphiesRateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                // Max 10 calls per second per tenant (adjust per NPHIES quota)
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                // Wait up to 2s for a permit before throwing RateLimiterException
                .timeoutDuration(Duration.ofSeconds(2))
                .build();

        return RateLimiterRegistry.of(config);
    }
}
