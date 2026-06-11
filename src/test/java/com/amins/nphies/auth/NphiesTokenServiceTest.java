package com.amins.nphies.auth;

import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.exception.NphiesAuthException;
import com.amins.nphies.security.VaultCredentialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NphiesTokenServiceTest {

    @Mock VaultCredentialService vaultCredentialService;

    private MockWebServer mockWebServer;
    private NphiesTokenService tokenService;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String TENANT = "tenant-A";
    private static final String CLIENT_SECRET = "plain-secret";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        tokenService = new NphiesTokenService(webClient, vaultCredentialService);

        when(vaultCredentialService.resolveClientSecret(eq(TENANT), any(), any()))
                .thenReturn(CLIENT_SECRET);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ── Successful token fetch ────────────────────────────────────────────────

    @Test
    void fetchToken_success_returnsValidToken() throws Exception {
        String tokenJson = objectMapper.writeValueAsString(Map.of(
                "access_token", "nphies-bearer-xyz",
                "token_type", "Bearer",
                "expires_in", 3600
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(tokenJson));

        TenantNphiesConfig config = buildConfig(TENANT);
        NphiesToken token = tokenService.fetchToken(config);

        assertThat(token.getAccessToken()).isEqualTo("nphies-bearer-xyz");
        assertThat(token.getTokenType()).isEqualTo("Bearer");
        assertThat(token.getTenantId()).isEqualTo(TENANT);
        assertThat(token.isExpiredOrExpiringSoon()).isFalse();
    }

    @Test
    void fetchToken_sendsCorrectFormData() throws Exception {
        enqueueSuccessToken();

        TenantNphiesConfig config = buildConfig(TENANT);
        tokenService.fetchToken(config);

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();

        assertThat(body).contains("grant_type=client_credentials");
        assertThat(body).contains("client_id=" + config.getClientId());
        assertThat(body).contains("client_secret=" + CLIENT_SECRET);
        assertThat(body).contains("scope=nphies");
    }

    @Test
    void fetchToken_defaultsExpiresInTo3600WhenMissing() throws Exception {
        String tokenJson = objectMapper.writeValueAsString(Map.of(
                "access_token", "tok"
                // no expires_in field
        ));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(tokenJson));

        NphiesToken token = tokenService.fetchToken(buildConfig(TENANT));
        // Token should expire roughly 1 hour from now (not expiring soon)
        assertThat(token.isExpiredOrExpiringSoon()).isFalse();
    }

    // ── 4xx errors ───────────────────────────────────────────────────────────

    @Test
    void fetchToken_401_throwsNphiesAuthException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized"));

        assertThatThrownBy(() -> tokenService.fetchToken(buildConfig(TENANT)))
                .isInstanceOf(NphiesAuthException.class)
                .hasMessageContaining(TENANT);
    }

    @Test
    void fetchToken_400_throwsNphiesAuthException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Bad Request - invalid client_id"));

        assertThatThrownBy(() -> tokenService.fetchToken(buildConfig(TENANT)))
                .isInstanceOf(NphiesAuthException.class);
    }

    // ── 5xx errors ───────────────────────────────────────────────────────────

    @Test
    void fetchToken_500_throwsNphiesAuthException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> tokenService.fetchToken(buildConfig(TENANT)))
                .isInstanceOf(NphiesAuthException.class)
                .hasMessageContaining(TENANT);
    }

    // ── Missing access_token in response ─────────────────────────────────────

    @Test
    void fetchToken_missingAccessToken_throwsNphiesAuthException() throws Exception {
        String tokenJson = objectMapper.writeValueAsString(Map.of("token_type", "Bearer"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(tokenJson));

        assertThatThrownBy(() -> tokenService.fetchToken(buildConfig(TENANT)))
                .isInstanceOf(NphiesAuthException.class)
                .hasMessageContaining("access_token");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void enqueueSuccessToken() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(Map.of(
                        "access_token", "tok",
                        "token_type", "Bearer",
                        "expires_in", 3600
                ))));
    }

    private TenantNphiesConfig buildConfig(String tenantId) {
        return TenantNphiesConfig.builder()
                .tenantId(tenantId)
                .clientId("client-" + tenantId)
                .clientSecretEncrypted("encrypted")
                .providerLicenseNo("LIC-001")
                .tokenEndpoint(mockWebServer.url("/oauth/token").toString())
                .apiBaseUrl(mockWebServer.url("/r4").toString())
                .environment(TenantNphiesConfig.NphiesEnvironment.UAT)
                .active(true)
                .build();
    }
}
