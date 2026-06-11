package com.amins.nphies.auth;

import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.exception.NphiesAuthException;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NphiesTokenStoreTest {

    @Mock NphiesTokenService tokenService;
    @Mock TenantNphiesConfigRepository configRepo;

    private NphiesTokenStore tokenStore;

    private static final String TENANT = "hospital-001";

    @BeforeEach
    void setUp() {
        tokenStore = new NphiesTokenStore(tokenService, configRepo);
    }

    // ── Cache miss: fetches and caches token ─────────────────────────────────

    @Test
    void getValidToken_cacheMiss_fetchesFromService() {
        TenantNphiesConfig config = buildConfig(TENANT);
        when(configRepo.findByTenantIdAndActiveTrue(TENANT)).thenReturn(Optional.of(config));
        when(tokenService.fetchToken(config)).thenReturn(freshToken(TENANT, 3600));

        String token = tokenStore.getValidToken(TENANT);

        assertThat(token).isEqualTo("access-token-" + TENANT);
        verify(tokenService, times(1)).fetchToken(config);
    }

    @Test
    void getValidToken_cacheHit_doesNotFetchAgain() {
        TenantNphiesConfig config = buildConfig(TENANT);
        when(configRepo.findByTenantIdAndActiveTrue(TENANT)).thenReturn(Optional.of(config));
        when(tokenService.fetchToken(config)).thenReturn(freshToken(TENANT, 3600));

        tokenStore.getValidToken(TENANT); // populates cache
        tokenStore.getValidToken(TENANT); // should hit cache

        verify(tokenService, times(1)).fetchToken(any()); // only fetched once
    }

    // ── Expired token triggers refresh ───────────────────────────────────────

    @Test
    void getValidToken_expiredToken_refreshes() {
        TenantNphiesConfig config = buildConfig(TENANT);
        when(configRepo.findByTenantIdAndActiveTrue(TENANT)).thenReturn(Optional.of(config));

        // First call returns an already-expired token
        NphiesToken expiredToken = NphiesToken.builder()
                .tenantId(TENANT).accessToken("old-token").tokenType("Bearer")
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(60)) // expired
                .build();

        NphiesToken freshToken = freshToken(TENANT, 3600);

        when(tokenService.fetchToken(config))
                .thenReturn(expiredToken)
                .thenReturn(freshToken);

        String first = tokenStore.getValidToken(TENANT);
        tokenStore.invalidateToken(TENANT); // simulate expiry-based eviction
        String second = tokenStore.getValidToken(TENANT);

        assertThat(first).isEqualTo("old-token");
        assertThat(second).isEqualTo("access-token-" + TENANT);
        verify(tokenService, times(2)).fetchToken(config);
    }

    // ── Invalidation ─────────────────────────────────────────────────────────

    @Test
    void invalidateToken_forcesNextCallToRefetch() {
        TenantNphiesConfig config = buildConfig(TENANT);
        when(configRepo.findByTenantIdAndActiveTrue(TENANT)).thenReturn(Optional.of(config));
        when(tokenService.fetchToken(config)).thenReturn(freshToken(TENANT, 3600));

        tokenStore.getValidToken(TENANT);
        tokenStore.invalidateToken(TENANT);
        tokenStore.getValidToken(TENANT);

        verify(tokenService, times(2)).fetchToken(config);
    }

    @Test
    void invalidateAll_clearsAllTenants() {
        String tenant2 = "clinic-002";
        TenantNphiesConfig cfg1 = buildConfig(TENANT);
        TenantNphiesConfig cfg2 = buildConfig(tenant2);

        when(configRepo.findByTenantIdAndActiveTrue(TENANT)).thenReturn(Optional.of(cfg1));
        when(configRepo.findByTenantIdAndActiveTrue(tenant2)).thenReturn(Optional.of(cfg2));
        when(tokenService.fetchToken(cfg1)).thenReturn(freshToken(TENANT, 3600));
        when(tokenService.fetchToken(cfg2)).thenReturn(freshToken(tenant2, 3600));

        tokenStore.getValidToken(TENANT);
        tokenStore.getValidToken(tenant2);
        tokenStore.invalidateAll();
        tokenStore.getValidToken(TENANT);
        tokenStore.getValidToken(tenant2);

        verify(tokenService, times(2)).fetchToken(cfg1);
        verify(tokenService, times(2)).fetchToken(cfg2);
    }

    // ── Error cases ──────────────────────────────────────────────────────────

    @Test
    void getValidToken_noActiveConfig_throwsNphiesAuthException() {
        when(configRepo.findByTenantIdAndActiveTrue(TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenStore.getValidToken(TENANT))
                .isInstanceOf(NphiesAuthException.class)
                .hasMessageContaining(TENANT);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private NphiesToken freshToken(String tenantId, long expiresIn) {
        return NphiesToken.of(tenantId, "access-token-" + tenantId, "Bearer", expiresIn);
    }

    private TenantNphiesConfig buildConfig(String tenantId) {
        return TenantNphiesConfig.builder()
                .tenantId(tenantId)
                .clientId("client-" + tenantId)
                .clientSecretEncrypted("encrypted-secret")
                .providerLicenseNo("LIC-001")
                .tokenEndpoint("https://auth.nphies.sa/token")
                .apiBaseUrl("https://HSB.nphies.sa/r4")
                .environment(TenantNphiesConfig.NphiesEnvironment.UAT)
                .active(true)
                .build();
    }
}
