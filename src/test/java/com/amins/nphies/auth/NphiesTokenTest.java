package com.amins.nphies.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NphiesTokenTest {

    @Test
    void of_setsFieldsCorrectly() {
        NphiesToken token = NphiesToken.of("tenant-1", "access-abc", "Bearer", 3600);

        assertThat(token.getTenantId()).isEqualTo("tenant-1");
        assertThat(token.getAccessToken()).isEqualTo("access-abc");
        assertThat(token.getTokenType()).isEqualTo("Bearer");
        assertThat(token.getIssuedAt()).isNotNull();
        assertThat(token.getExpiresAt()).isAfter(token.getIssuedAt());
    }

    @Test
    void of_defaultsTokenTypeToBearerWhenNull() {
        NphiesToken token = NphiesToken.of("t", "tok", null, 3600);
        assertThat(token.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void isExpiredOrExpiringSoon_falseWhenExpiryFarInFuture() {
        NphiesToken token = NphiesToken.of("t", "tok", "Bearer", 3600); // expires in 1 hour
        assertThat(token.isExpiredOrExpiringSoon()).isFalse();
    }

    @Test
    void isExpiredOrExpiringSoon_trueWhenAlreadyExpired() {
        NphiesToken token = NphiesToken.builder()
                .tenantId("t")
                .accessToken("tok")
                .tokenType("Bearer")
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(10)) // expired 10s ago
                .build();

        assertThat(token.isExpiredOrExpiringSoon()).isTrue();
    }

    @Test
    void isExpiredOrExpiringSoon_trueWhenWithin5MinuteBuffer() {
        NphiesToken token = NphiesToken.builder()
                .tenantId("t")
                .accessToken("tok")
                .tokenType("Bearer")
                .issuedAt(Instant.now().minusSeconds(3300))
                .expiresAt(Instant.now().plusSeconds(240)) // expires in 4 min — inside 5-min buffer
                .build();

        assertThat(token.isExpiredOrExpiringSoon()).isTrue();
    }

    @Test
    void isExpiredOrExpiringSoon_falseWhenJustOutsideBuffer() {
        NphiesToken token = NphiesToken.builder()
                .tenantId("t")
                .accessToken("tok")
                .tokenType("Bearer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(360)) // 6 minutes — outside 5-min buffer
                .build();

        assertThat(token.isExpiredOrExpiringSoon()).isFalse();
    }
}
