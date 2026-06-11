package com.amins.nphies.security;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Map;
import java.util.Optional;

/**
 * Abstracts external secret store access (GCP Secret Manager or HashiCorp Vault).
 *
 * Priority order for resolving client_secret per tenant:
 *   1. External secret ref (GCP resource name or Vault path) — if set on tenant config
 *   2. AES-encrypted column in DB                            — fallback
 *
 * External secret JSON format (both stores):
 *   { "client_id": "...", "client_secret": "..." }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VaultCredentialService {

    private final Optional<SecretManagerServiceClient> gcpSecretManagerClient;
    private final Optional<VaultTemplate>     vaultTemplate;
    private final AesEncryptionService        aesEncryptionService;
    private final ObjectMapper                objectMapper;

    /**
     * Resolves the plaintext client_secret for a tenant.
     *
     * @param tenantId             for logging
     * @param externalSecretRef    GCP resource name like "projects/{id}/secrets/{name}/versions/latest"
     *                             or Vault path like "secret/nphies/tenantX"
     *                             Pass null to fall back to AES-encrypted column
     * @param clientSecretEncrypted AES-encrypted fallback value from DB
     */
    public String resolveClientSecret(
            String tenantId,
            String externalSecretRef,
            String clientSecretEncrypted) {

        if (externalSecretRef != null && !externalSecretRef.isBlank()) {
            if (externalSecretRef.startsWith("projects/")) {
                return fetchFromGcpSecretManager(tenantId, externalSecretRef);
            } else {
                return fetchFromVault(tenantId, externalSecretRef);
            }
        }

        // Fallback: decrypt AES-encrypted value from DB
        log.debug("Resolving client_secret from AES-encrypted DB column for tenant: {}", tenantId);
        return aesEncryptionService.decrypt(clientSecretEncrypted);
    }

    // ── GCP Secret Manager ───────────────────────────────────────────────────

    private String fetchFromGcpSecretManager(String tenantId, String secretVersionName) {
        SecretManagerServiceClient client = gcpSecretManagerClient.orElseThrow(() ->
                new IllegalStateException("GCP Secret Manager not configured. " +
                        "Set gcp.project-id in application.yml"));
        try {
            log.debug("Fetching NPHIES secret from GCP Secret Manager for tenant: {}", tenantId);
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            String secretJson = response.getPayload().getData().toStringUtf8();

            Map<?, ?> secretMap = objectMapper.readValue(secretJson, Map.class);
            String secret = (String) secretMap.get("client_secret");

            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "GCP secret missing 'client_secret' key for tenant: " + tenantId);
            }
            return secret;

        } catch (Exception e) {
            log.error("Failed to fetch secret from GCP Secret Manager for tenant: {}", tenantId, e);
            throw new NphiesCredentialResolutionException(
                    "Cannot resolve NPHIES credentials for tenant: " + tenantId, e);
        }
    }

    // ── HashiCorp Vault ──────────────────────────────────────────────────────

    private String fetchFromVault(String tenantId, String vaultPath) {
        VaultTemplate vault = vaultTemplate.orElseThrow(() ->
                new IllegalStateException("HashiCorp Vault not configured. " +
                        "Set spring.cloud.vault in application.yml"));

        try {
            log.debug("Fetching NPHIES secret from Vault for tenant: {}", tenantId);
            VaultResponse response = vault.read(vaultPath);

            if (response == null || response.getData() == null) {
                throw new IllegalStateException(
                        "Vault returned empty response for path: " + vaultPath);
            }

            String secret = (String) response.getData().get("client_secret");
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "Vault secret missing 'client_secret' key at: " + vaultPath);
            }
            return secret;

        } catch (Exception e) {
            log.error("Failed to fetch secret from Vault for tenant: {}", tenantId, e);
            throw new NphiesCredentialResolutionException(
                    "Cannot resolve NPHIES credentials for tenant: " + tenantId, e);
        }
    }

    // ── Exception ────────────────────────────────────────────────────────────

    public static class NphiesCredentialResolutionException extends RuntimeException {
        public NphiesCredentialResolutionException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
