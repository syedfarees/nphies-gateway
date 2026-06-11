package com.amins.nphies.auth;

import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.exception.NphiesAuthException;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Per-tenant OAuth2 token cache backed by Caffeine.
 *
 * Design decisions:
 *  - Per-entry TTL based on actual token expiry (not fixed window)
 *  - ReentrantLock per tenant prevents thundering herd on token refresh
 *  - Automatic eviction on expiry — no background thread needed
 *  - Cache invalidation exposed for forced refresh (e.g. after credential rotation)
 */
@Component
@Slf4j
public class NphiesTokenStore {

    private final Cache<String, NphiesToken> tokenCache;
    private final NphiesTokenService         tokenService;
    private final TenantNphiesConfigRepository configRepo;

    // Per-tenant locks prevent multiple simultaneous token refreshes for same tenant
    private final Map<String, ReentrantLock> tenantLocks = new ConcurrentHashMap<>();

    public NphiesTokenStore(NphiesTokenService tokenService,
                            TenantNphiesConfigRepository configRepo) {
        this.tokenService = tokenService;
        this.configRepo   = configRepo;

        // Maximum 1000 tenants in cache. In practice much lower.
        // expireAfterWrite is a safety net; token.isExpiredOrExpiringSoon() is primary check.
        this.tokenCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(65)) // Safety: most NPHIES tokens last 60 min
                .recordStats()                             // Expose hit/miss to Micrometer
                .removalListener((key, value, cause) ->
                        log.debug("Token evicted for tenant: {} reason: {}", key, cause))
                .build();
    }

    /**
     * Gets a valid token for the given tenant.
     * Fetches a new one if absent or expiring within 5 minutes.
     * Thread-safe: only one refresh per tenant at a time.
     *
     * @param tenantId the tenant requiring a token
     * @return valid Bearer access token string
     */
    public String getValidToken(String tenantId) {
        NphiesToken cached = tokenCache.getIfPresent(tenantId);

        if (cached != null && !cached.isExpiredOrExpiringSoon()) {
            log.debug("Token cache HIT for tenant: {}", tenantId);
            return cached.getAccessToken();
        }

        // Token absent or expiring — acquire tenant-scoped lock and refresh
        ReentrantLock lock = tenantLocks.computeIfAbsent(tenantId, k -> new ReentrantLock());
        lock.lock();
        try {
            // Double-check after acquiring lock (another thread may have refreshed)
            NphiesToken recheck = tokenCache.getIfPresent(tenantId);
            if (recheck != null && !recheck.isExpiredOrExpiringSoon()) {
                log.debug("Token refreshed by another thread for tenant: {}", tenantId);
                return recheck.getAccessToken();
            }

            log.info("Token cache MISS — refreshing for tenant: {}", tenantId);
            NphiesToken fresh = refreshToken(tenantId);
            tokenCache.put(tenantId, fresh);
            return fresh.getAccessToken();

        } finally {
            lock.unlock();
        }
    }

    /**
     * Forces token eviction and immediate re-fetch.
     * Use after credential rotation or when NPHIES returns 401.
     */
    public void invalidateToken(String tenantId) {
        log.info("Invalidating NPHIES token for tenant: {}", tenantId);
        tokenCache.invalidate(tenantId);
    }

    /**
     * Invalidates tokens for ALL tenants. Use with extreme caution.
     */
    public void invalidateAll() {
        log.warn("Invalidating ALL NPHIES tokens — {} tenants affected", tokenCache.estimatedSize());
        tokenCache.invalidateAll();
    }

    /** Cache stats for Micrometer/Actuator exposure */
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return tokenCache.stats();
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private NphiesToken refreshToken(String tenantId) {
        TenantNphiesConfig config = configRepo.findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new NphiesAuthException(
                        "No active NPHIES configuration found for tenant: " + tenantId));

        return tokenService.fetchToken(config);
    }
}
