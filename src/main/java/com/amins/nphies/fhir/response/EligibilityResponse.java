package com.amins.nphies.fhir.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class EligibilityResponse {
    String             requestId;
    String             bundleId;          // NPHIES response bundle ID
    String             responseCode;      // MessageHeader.response.code ("ok", "transient-error", …)
    EligibilityOutcome outcome;
    String             disposition;
    boolean            inforce;
    String             siteEligibilityCode;    // extension-siteEligibility code
    LocalDate          servicedPeriodStart;    // CoverageEligibilityResponse.servicedPeriod.start
    LocalDate          servicedPeriodEnd;      // CoverageEligibilityResponse.servicedPeriod.end
    String             coverageType;           // Coverage.type.coding[0].code
    LocalDate          coveragePeriodStart;    // Coverage.period.start
    LocalDate          coveragePeriodEnd;      // Coverage.period.end
    List<BenefitItem>  benefits;               // never null, may be empty
    String             rawResponseJson;

    public enum EligibilityOutcome { COMPLETE, QUEUED, ERROR, PARTIAL, PENDING }

    @Value
    @Builder
    public static class BenefitItem {
        String category;
        String network;
        String term;
        String benefitType;
        String allowedValue;
        String usedValue;
    }
}
