package com.amins.nphies.config;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Provides a SecretManagerServiceClient bean for GCP Secret Manager access.
 *
 * Authentication uses Application Default Credentials (ADC):
 *  - GCP environments (GCE/GKE/Cloud Run): resolved via metadata server automatically.
 *  - Local dev: set GOOGLE_APPLICATION_CREDENTIALS env var or run
 *    `gcloud auth application-default login`.
 *
 * Bean is only created when gcp.secret-manager.enabled=true (default false, so
 * local dev boots without GCP credentials), matching the Optional-injection
 * pattern used for HashiCorp Vault.
 */
@Configuration
@Slf4j
public class GcpSecretManagerConfig {

    @Bean
    @ConditionalOnProperty(name = "gcp.secret-manager.enabled", havingValue = "true")
    public SecretManagerServiceClient secretManagerServiceClient() throws IOException {
        log.info("Initializing GCP SecretManagerServiceClient (ADC auth)");
        return SecretManagerServiceClient.create();
    }
}
