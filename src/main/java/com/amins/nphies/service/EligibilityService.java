package com.amins.nphies.service;

import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.exception.NphiesException;
import com.amins.nphies.fhir.bundle.CoverageEligibilityRequestBundleBuilder;
import com.amins.nphies.fhir.bundle.EligibilityRequestInput;
import com.amins.nphies.fhir.response.CoverageEligibilityResponseMapper;
import com.amins.nphies.fhir.response.EligibilityResponse;
import com.amins.nphies.gateway.NphiesGatewayClient;
import com.amins.nphies.model.TenantContext;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a NPHIES coverage eligibility check for a given tenant.
 *
 * Flow:
 *   1. Load active tenant configuration
 *   2. Map service-level request → FHIR input (injecting provider fields from config)
 *   3. Build FHIR Bundle JSON
 *   4. Submit to NPHIES via gateway
 *   5. Parse and return mapped response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EligibilityService {

    private final TenantNphiesConfigRepository        configRepository;
    private final CoverageEligibilityRequestBundleBuilder bundleBuilder;
    private final NphiesGatewayClient                 gatewayClient;
    private final CoverageEligibilityResponseMapper   responseMapper;

    public EligibilityResponse checkEligibility(EligibilityCheckRequest request) {
        String tenantId = TenantContext.require();
        log.info("Checking eligibility for tenant: {} requestId: {}", tenantId, request.getRequestId());

        TenantNphiesConfig config = configRepository
                .findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new NphiesException(
                        "No active NPHIES configuration for tenant: " + tenantId));

        EligibilityRequestInput fhirInput = EligibilityRequestInput.builder()
                .requestId(request.getRequestId())
                .providerBaseUrl(request.getProviderBaseUrl())
                // Patient
                .patientNationalId(request.getPatientNationalId())
                .patientFirstName(request.getPatientFirstName())
                .patientGivenNames(request.getPatientGivenNames())
                .patientFamilyName(request.getPatientFamilyName())
                .patientDateOfBirth(request.getPatientDateOfBirth())
                .patientGender(request.getPatientGender())
                .patientPhone(request.getPatientPhone())
                .patientMaritalStatus(request.getPatientMaritalStatus())
                .patientNationalityCode(request.getPatientNationalityCode())
                .patientNationalityDisplay(request.getPatientNationalityDisplay())
                // Coverage
                .memberId(request.getMemberId())
                .memberIdSystem(request.getMemberIdSystem())
                .coverageRelationship(request.getCoverageRelationship())
                .coverageType(request.getCoverageType())
                .coverageTypeDisplay(request.getCoverageTypeDisplay())
                .coveragePeriodStart(request.getCoveragePeriodStart())
                .coveragePeriodEnd(request.getCoveragePeriodEnd())
                .businessArrangement(request.getBusinessArrangement())
                // Payer
                .payerLicenseNumber(request.getPayerLicenseNumber())
                .payerName(request.getPayerName())
                // Provider — always from tenant config, never from client
                .providerLicenseNumber(config.getProviderLicenseNo())
                .providerName(config.getTenantId())
                // Request parameters
                .servicedDate(request.getServicedDate())
                .servicedPeriodEnd(request.getServicedPeriodEnd())
                .purposes(request.getPurposes())
                .build();

        String bundleJson   = bundleBuilder.build(fhirInput);
        String responseJson = gatewayClient.submitBundle(tenantId, config.getApiBaseUrl(), bundleJson);

        return responseMapper.map(request.getRequestId(), responseJson);
    }
}
