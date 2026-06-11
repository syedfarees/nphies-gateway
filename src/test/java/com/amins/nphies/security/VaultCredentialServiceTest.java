package com.amins.nphies.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultCredentialServiceTest {

    @Mock SecretManagerServiceClient gcpSecretManagerClient;

    private AesEncryptionService aesEncryptionService;
    private VaultCredentialService service;

    private static final String TENANT = "tenant-X";
    private static final String PLAIN_SECRET = "my-plain-secret";

    @BeforeEach
    void setUp() {
        aesEncryptionService = new AesEncryptionService(
                Base64.getEncoder().encodeToString(new byte[32])
        );
        service = new VaultCredentialService(
                Optional.empty(),       // no GCP Secret Manager
                Optional.empty(),       // no Vault
                aesEncryptionService,
                new ObjectMapper()
        );
    }

    // ── AES fallback ─────────────────────────────────────────────────────────

    @Test
    void resolveClientSecret_noExternalRef_decryptsFromDb() {
        String encrypted = aesEncryptionService.encrypt(PLAIN_SECRET);

        String resolved = service.resolveClientSecret(TENANT, null, encrypted);

        assertThat(resolved).isEqualTo(PLAIN_SECRET);
    }

    @Test
    void resolveClientSecret_blankExternalRef_decryptsFromDb() {
        String encrypted = aesEncryptionService.encrypt(PLAIN_SECRET);

        String resolved = service.resolveClientSecret(TENANT, "  ", encrypted);

        assertThat(resolved).isEqualTo(PLAIN_SECRET);
    }

    // ── GCP SM path detection ─────────────────────────────────────────────────

    @Test
    void resolveClientSecret_gcpRefWithNoClient_throwsIllegalState() {
        String gcpRef = "projects/my-project/secrets/nphies-tenantX/versions/latest";
        assertThatThrownBy(() -> service.resolveClientSecret(TENANT, gcpRef, "encrypted"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GCP Secret Manager not configured");
    }

    // ── HashiCorp Vault path detection ────────────────────────────────────────

    @Test
    void resolveClientSecret_vaultPathWithNoClient_throwsIllegalState() {
        // Vault template not configured (Optional.empty()) — should throw
        String vaultPath = "secret/nphies/tenantX";

        assertThatThrownBy(() -> service.resolveClientSecret(TENANT, vaultPath, "encrypted"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HashiCorp Vault not configured");
    }

    // ── GCP SM integration (with mock) ────────────────────────────────────────

    @Test
    void resolveClientSecret_gcpRef_fetchesFromSecretManager() throws Exception {
        String gcpRef = "projects/my-project/secrets/nphies-tenantX/versions/latest";
        String secretJson = new ObjectMapper().writeValueAsString(
                java.util.Map.of("client_id", "cid", "client_secret", PLAIN_SECRET));

        AccessSecretVersionResponse response = AccessSecretVersionResponse.newBuilder()
                .setPayload(SecretPayload.newBuilder()
                        .setData(ByteString.copyFromUtf8(secretJson)).build())
                .build();
        when(gcpSecretManagerClient.accessSecretVersion(gcpRef)).thenReturn(response);

        VaultCredentialService serviceWithGcp = new VaultCredentialService(
                Optional.of(gcpSecretManagerClient), Optional.empty(),
                aesEncryptionService, new ObjectMapper());

        assertThat(serviceWithGcp.resolveClientSecret(TENANT, gcpRef, "encrypted"))
                .isEqualTo(PLAIN_SECRET);
    }

    @Test
    void resolveClientSecret_gcpRef_missingClientSecretKey_throwsException() throws Exception {
        String gcpRef = "projects/my-project/secrets/nphies-tenantX/versions/latest";
        String secretJson = new ObjectMapper().writeValueAsString(Map.of("client_id", "cid"));

        AccessSecretVersionResponse response = AccessSecretVersionResponse.newBuilder()
                .setPayload(SecretPayload.newBuilder()
                        .setData(ByteString.copyFromUtf8(secretJson)).build())
                .build();
        when(gcpSecretManagerClient.accessSecretVersion(gcpRef)).thenReturn(response);

        VaultCredentialService serviceWithGcp = new VaultCredentialService(
                Optional.of(gcpSecretManagerClient), Optional.empty(),
                aesEncryptionService, new ObjectMapper());

        assertThatThrownBy(() -> serviceWithGcp.resolveClientSecret(TENANT, gcpRef, "encrypted"))
                .isInstanceOf(VaultCredentialService.NphiesCredentialResolutionException.class);
    }
}
