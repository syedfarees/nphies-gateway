package com.amins.nphies.auth;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Immutable model holding a tenant's NPHIES OAuth2 access token.
 * Refresh is triggered when isExpired() returns true (5 min before actual expiry).
 */
@Getter
@Builder
public class NphiesToken {

    private final String  accessToken;
    private final String  tokenType;    // always "Bearer"
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final String  tenantId;

    /**
     * Returns true if token has expired or will expire within 5 minutes.
     * 5-minute buffer prevents mid-request expiry during long FHIR operations.
     */
    public boolean isExpiredOrExpiringSoon() {
        return Instant.now().isAfter(expiresAt.minusSeconds(300));
    }

    public static NphiesToken of(String tenantId, String accessToken,
                                 String tokenType, long expiresInSeconds) {
        Instant now = Instant.now();
        return NphiesToken.builder()
                .tenantId(tenantId)
                .accessToken(accessToken)
                .tokenType(tokenType != null ? tokenType : "Bearer")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .build();
    }
}
