package com.amins.nphies.fhir.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class EligibilityResponse {
    String             requestId;
    EligibilityOutcome outcome;
    String             disposition;
    boolean            inforce;
    List<BenefitItem>  benefits;       // never null, may be empty
    String             rawResponseJson;

    public enum EligibilityOutcome { COMPLETE, QUEUED, ERROR, PARTIAL, PENDING }

    @Value
    @Builder
    public static class BenefitItem {
        String category;      // text or display or code from CodeableConcept
        String network;
        String term;
        String benefitType;
        String allowedValue;  // Money, integer, or string — always stringified
        String usedValue;
    }
}
