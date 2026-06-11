package com.amins.nphies.auth;

import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.exception.NphiesAuthException;
import com.amins.nphies.security.VaultCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Fetches fresh OAuth2 Bearer tokens from NPHIES token endpoint.
 * Uses client_credentials grant type (machine-to-machine, no user involved).
 *
 * Called by NphiesTokenStore on cache miss or token expiry.
 * Never called directly — always go through NphiesTokenStore.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NphiesTokenService {

    private final WebClient           plainWebClient; // no-auth client for token endpoint
    private final VaultCredentialService vaultCredentialService;

    /**
     * Performs the OAuth2 client_credentials token exchange.
     *
     * @param config tenant's NPHIES configuration
     * @return fresh NphiesToken
     */
    public NphiesToken fetchToken(TenantNphiesConfig config) {
        String tenantId = config.getTenantId();
        log.info("Fetching new NPHIES OAuth2 token for tenant: {}", tenantId);

        // Resolve plaintext secret from vault/AES — NEVER log this
        String clientSecret = vaultCredentialService.resolveClientSecret(
                tenantId,
                config.getExternalSecretRef(),
                config.getClientSecretEncrypted()
        );

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type",    "client_credentials");
        formData.add("client_id",     config.getClientId());
        formData.add("client_secret", clientSecret);
        formData.add("scope",         "nphies");   // NPHIES-required scope

        try {
            Map<?, ?> tokenResponse = plainWebClient.post()
                    .uri(config.getTokenEndpoint())
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            response -> response.bodyToMono(String.class).flatMap(body -> {
                                log.error("NPHIES token request rejected for tenant: {}. Status: {}, Body: {}",
                                        tenantId, response.statusCode(), body);
                                return Mono.error(new NphiesAuthException(
                                        "NPHIES token endpoint rejected credentials for tenant: " + tenantId +
                                                ". Status: " + response.statusCode()));
                            })
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> Mono.error(new NphiesAuthException(
                                    "NPHIES token endpoint server error for tenant: " + tenantId))
                    )
                    .bodyToMono(Map.class)
                    .block(); // Blocking here is intentional — token fetch is init path

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new NphiesAuthException(
                        "NPHIES token response missing access_token for tenant: " + tenantId);
            }

            long expiresIn = tokenResponse.containsKey("expires_in")
                    ? Long.parseLong(tokenResponse.get("expires_in").toString())
                    : 3600L; // default 1 hour

            NphiesToken token = NphiesToken.of(
                    tenantId,
                    (String) tokenResponse.get("access_token"),
                    tokenResponse.containsKey("token_type") ? (String) tokenResponse.get("token_type") : "Bearer",
                    expiresIn
            );

            log.info("Successfully obtained NPHIES token for tenant: {}. Expires in: {}s",
                    tenantId, expiresIn);
            return token;

        } catch (NphiesAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new NphiesAuthException(
                    "Unexpected error fetching NPHIES token for tenant: " + tenantId, e);
        } finally {
            // Zero out the secret reference (GC hint)
            clientSecret = null;
        }
    }
}
