package com.amins.nphies.beneficiary;

import com.amins.nphies.beneficiary.dto.TracareTenantDto;
import com.amins.nphies.beneficiary.dto.TracarePatientDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
@Slf4j
public class TracareAppClient {

    private final WebClient webClient;
    private final String internalApiKey;

    public TracareAppClient(
            @Value("${tracare.base-url}") String baseUrl,
            @Value("${tracare.internal-api-key}") String internalApiKey) {
        this.webClient = WebClient.create(baseUrl);
        this.internalApiKey = internalApiKey;
        log.info("TracareAppClient configured — base-url={}", baseUrl);
    }

    public Optional<TracareTenantDto> lookupTenant(String tenantId) {
        try {
            TracareTenantDto tenant = webClient.get()
                    .uri("/tracare/internal/tenant/lookup?tenantId={id}", tenantId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(TracareTenantDto.class);
                        }
                        log.debug("TraCare tenant lookup — HTTP {}", response.statusCode());
                        return response.releaseBody().thenReturn((TracareTenantDto) null);
                    })
                    .block();
            return Optional.ofNullable(tenant);
        } catch (Exception e) {
            log.warn("TraCare tenant lookup failed for tenantId={}: [{}] {}",
                    tenantId, e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<TracarePatientDto> findPatientByIdNumber(String idNumber, String tenantId) {
        try {
            log.debug("TraCare patient lookup — idNumber={}, tenantId={}", idNumber, tenantId);
            TracarePatientDto patient = webClient.get()
                    .uri("/tracare/internal/patient/by-id?idNumber={id}", idNumber)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .header("X-Tenant-Id", tenantId)
                    .exchangeToMono(response -> {
                        log.debug("TraCare patient lookup — HTTP {}", response.statusCode());
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(TracarePatientDto.class);
                        }
                        return response.releaseBody().thenReturn((TracarePatientDto) null);
                    })
                    .block();
            return Optional.ofNullable(patient);
        } catch (Exception e) {
            log.warn("TraCare lookup failed for idNumber={}, tenantId={}: [{}] {}",
                    idNumber, tenantId, e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }
}
