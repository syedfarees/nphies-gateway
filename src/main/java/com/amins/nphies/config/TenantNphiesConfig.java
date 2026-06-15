package com.amins.nphies.config;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Stores per-tenant NPHIES credentials and configuration.
 * client_secret is stored AES-256-GCM encrypted — NEVER plaintext.
 * One row per tenant (hospital / clinic facility).
 */
@Entity
@Table(name = "tenant_nphies_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantNphiesConfig {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    /** NPHIES-issued OAuth2 client_id for this provider */
    @NotBlank
    @Column(name = "client_id", nullable = false)
    private String clientId;

    /**
     * AES-256-GCM encrypted client_secret.
     * Never expose raw — always decrypt via AesEncryptionService.
     * Column length 1024 to accommodate IV + ciphertext + auth tag (Base64).
     */
    @NotBlank
    @Column(name = "client_secret_encrypted", nullable = false, length = 1024)
    private String clientSecretEncrypted;

    /**
     * Provider License Number — embedded in every FHIR bundle.
     * Issued by CCHI upon provider registration with NPHIES.
     */
    @NotBlank
    @Column(name = "provider_license_no", nullable = false)
    private String providerLicenseNo;

    /** OAuth2 token endpoint URL (UAT vs Production differs) */
    @NotBlank
    @Column(name = "token_endpoint", nullable = false)
    private String tokenEndpoint;

    /** NPHIES FHIR API base URL */
    @NotBlank
    @Column(name = "api_base_url", nullable = false)
    private String apiBaseUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "environment", nullable = false)
    private NphiesEnvironment environment;

    /** Polling interval in seconds. Min 30, default 60 */
    @Column(name = "polling_interval_sec", nullable = false)
    @Builder.Default
    private int pollingIntervalSec = 60;

    /** Disable a tenant without deleting their config */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Optional: external secret reference path (e.g. AWS SM ARN or Vault path).
     * If set, AES-encrypted column is ignored and secret is fetched from external vault.
     */
    @Column(name = "external_secret_ref")
    private String externalSecretRef;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum NphiesEnvironment {
        UAT, PRODUCTION
    }
}
