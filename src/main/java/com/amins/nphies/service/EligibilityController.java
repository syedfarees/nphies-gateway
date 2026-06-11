package com.amins.nphies.service;

import com.amins.nphies.beneficiary.BeneficiaryService;
import com.amins.nphies.beneficiary.dto.BeneficiaryResponse;
import com.amins.nphies.fhir.response.EligibilityResponse;
import com.amins.nphies.model.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/eligibility")
@RequiredArgsConstructor
public class EligibilityController {

    private final EligibilityService    eligibilityService;
    private final BeneficiaryService    beneficiaryService;

    @PostMapping("/check")
    public EligibilityResponse check(@Valid @RequestBody EligibilityCheckApiRequest req) {
        String tenantId = TenantContext.require();

        BeneficiaryResponse b = beneficiaryService.getById(tenantId, req.getBeneficiaryId());

        // Resolve payer fields: request overrides stored beneficiary value
        String memberId      = coalesce(req.getMemberId(),       b.getMemberId());
        String payerLicense  = coalesce(req.getPayerLicenseNo(), b.getPayerLicenseNo());
        String payerName     = coalesce(req.getPayerName(),      b.getPayerName());

        EligibilityCheckRequest checkRequest = EligibilityCheckRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .patientNationalId(b.getNationalId())
                .patientFirstName(b.getFirstName())
                .patientFamilyName(b.getFamilyName())
                .patientDateOfBirth(b.getDateOfBirth())
                .patientGender(normalizeGender(b.getGender()))
                .memberId(memberId != null ? memberId : "")
                .coverageRelationship(b.getCoverageRelationship())
                .payerLicenseNumber(payerLicense != null ? payerLicense : "")
                .payerName(payerName != null ? payerName : "")
                .servicedDate(req.getServicedDate())
                .build();

        return eligibilityService.checkEligibility(tenantId, checkRequest);
    }

    private static String coalesce(String override, String fallback) {
        return (override != null && !override.isBlank()) ? override : fallback;
    }

    private static String normalizeGender(String raw) {
        if (raw == null) return "unknown";
        return switch (raw.trim().toLowerCase()) {
            case "male",   "m" -> "male";
            case "female", "f" -> "female";
            default            -> "unknown";
        };
    }
}
