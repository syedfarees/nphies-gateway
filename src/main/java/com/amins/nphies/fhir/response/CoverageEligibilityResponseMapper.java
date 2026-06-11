package com.amins.nphies.fhir.response;

import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a NPHIES FHIR Bundle (containing a CoverageEligibilityResponse) to
 * the internal {@link EligibilityResponse} DTO.
 *
 * Returns a PENDING response when:
 * - responseJson is blank (gateway contract for not-yet-available responses)
 * - The Bundle contains no CoverageEligibilityResponse entry
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoverageEligibilityResponseMapper {

    private final IParser fhirJsonParser;

    public EligibilityResponse map(String requestId, String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            return pendingResponse(requestId, responseJson);
        }

        Bundle bundle = fhirJsonParser.parseResource(Bundle.class, responseJson);

        CoverageEligibilityResponse cerResponse = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof CoverageEligibilityResponse)
                .map(r -> (CoverageEligibilityResponse) r)
                .findFirst()
                .orElse(null);

        if (cerResponse == null) {
            log.warn("No CoverageEligibilityResponse found in Bundle for requestId: {}", requestId);
            return pendingResponse(requestId, responseJson);
        }

        String outcomeStr = cerResponse.getOutcomeElement().getValueAsString();
        EligibilityResponse.EligibilityOutcome outcome = mapOutcome(outcomeStr);

        boolean inforce = false;
        if (!cerResponse.getInsurance().isEmpty()) {
            inforce = cerResponse.getInsuranceFirstRep().getInforce();
        }

        String disposition = cerResponse.getDisposition();
        List<EligibilityResponse.BenefitItem> benefits = mapBenefits(cerResponse.getInsurance());

        return EligibilityResponse.builder()
                .requestId(requestId)
                .outcome(outcome)
                .disposition(disposition)
                .inforce(inforce)
                .benefits(benefits)
                .rawResponseJson(responseJson)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private EligibilityResponse pendingResponse(String requestId, String responseJson) {
        return EligibilityResponse.builder()
                .requestId(requestId)
                .outcome(EligibilityResponse.EligibilityOutcome.PENDING)
                .inforce(false)
                .benefits(List.of())
                .rawResponseJson(responseJson)
                .build();
    }

    private EligibilityResponse.EligibilityOutcome mapOutcome(String outcomeStr) {
        if (outcomeStr == null) {
            log.warn("Null NPHIES outcome — defaulting to PENDING");
            return EligibilityResponse.EligibilityOutcome.PENDING;
        }
        return switch (outcomeStr) {
            case "complete" -> EligibilityResponse.EligibilityOutcome.COMPLETE;
            case "queued"   -> EligibilityResponse.EligibilityOutcome.QUEUED;
            case "error"    -> EligibilityResponse.EligibilityOutcome.ERROR;
            case "partial"  -> EligibilityResponse.EligibilityOutcome.PARTIAL;
            default -> {
                log.warn("Unknown NPHIES outcome '{}' — defaulting to PENDING", outcomeStr);
                yield EligibilityResponse.EligibilityOutcome.PENDING;
            }
        };
    }

    private String extractCodeableConceptText(CodeableConcept cc) {
        if (cc == null) return null;
        if (cc.getText() != null && !cc.getText().isBlank()) return cc.getText();
        if (!cc.getCoding().isEmpty()) {
            String display = cc.getCodingFirstRep().getDisplay();
            if (display != null && !display.isBlank()) return display;
            String code = cc.getCodingFirstRep().getCode();
            if (code != null && !code.isBlank()) return code;
        }
        return null;
    }

    private String extractBenefitAmount(Type value) {
        if (value == null) return null;
        if (value instanceof UnsignedIntType u) return String.valueOf(u.getValue());
        if (value instanceof StringType s) return s.getValue();
        if (value instanceof Money m) return m.getValue() + " " + m.getCurrency();
        return null;
    }

    private List<EligibilityResponse.BenefitItem> mapBenefits(
            List<CoverageEligibilityResponse.InsuranceComponent> insuranceList) {

        List<EligibilityResponse.BenefitItem> result = new ArrayList<>();

        for (CoverageEligibilityResponse.InsuranceComponent insurance : insuranceList) {
            for (CoverageEligibilityResponse.ItemsComponent item : insurance.getItem()) {
                String category = extractCodeableConceptText(item.getCategory());
                String network  = extractCodeableConceptText(item.getNetwork());
                String term     = extractCodeableConceptText(item.getTerm());

                List<CoverageEligibilityResponse.BenefitComponent> benefitList = item.getBenefit();
                if (benefitList.isEmpty()) {
                    result.add(EligibilityResponse.BenefitItem.builder()
                            .category(category)
                            .network(network)
                            .term(term)
                            .build());
                } else {
                    for (CoverageEligibilityResponse.BenefitComponent benefit : benefitList) {
                        result.add(EligibilityResponse.BenefitItem.builder()
                                .category(category)
                                .network(network)
                                .term(term)
                                .benefitType(extractCodeableConceptText(benefit.getType()))
                                .allowedValue(extractBenefitAmount(benefit.getAllowed()))
                                .usedValue(extractBenefitAmount(benefit.getUsed()))
                                .build());
                    }
                }
            }
        }
        return result;
    }
}
