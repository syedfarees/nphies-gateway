package com.amins.nphies.fhir.response;

import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.fhir.NphiesProfiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
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
            return pendingResponse(requestId, responseJson, null);
        }

        Bundle bundle = fhirJsonParser.parseResource(Bundle.class, responseJson);
        String bundleId = bundle.getIdElement().getIdPart();

        // MessageHeader.response.code ("ok", "transient-error", "fatal-error")
        String responseCode = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof MessageHeader)
                .map(r -> (MessageHeader) r)
                .findFirst()
                .map(hdr -> hdr.hasResponse() && hdr.getResponse().getCode() != null
                        ? hdr.getResponse().getCode().toCode() : null)
                .orElse(null);

        CoverageEligibilityResponse cerResponse = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof CoverageEligibilityResponse)
                .map(r -> (CoverageEligibilityResponse) r)
                .findFirst()
                .orElse(null);

        if (cerResponse == null) {
            log.warn("No CoverageEligibilityResponse found in Bundle for requestId: {}", requestId);
            return pendingResponse(requestId, responseJson, bundleId);
        }

        String outcomeStr = cerResponse.getOutcomeElement().getValueAsString();
        EligibilityResponse.EligibilityOutcome outcome = mapOutcome(outcomeStr);

        boolean inforce = false;
        if (!cerResponse.getInsurance().isEmpty()) {
            inforce = cerResponse.getInsuranceFirstRep().getInforce();
        }

        String disposition = cerResponse.getDisposition();
        List<EligibilityResponse.BenefitItem> benefits = mapBenefits(cerResponse.getInsurance());

        // extension-siteEligibility
        String siteEligibilityCode = null;
        for (Extension ext : cerResponse.getExtension()) {
            if (NphiesProfiles.EXT_SITE_ELIGIBILITY.equals(ext.getUrl())
                    && ext.getValue() instanceof CodeableConcept cc
                    && !cc.getCoding().isEmpty()) {
                siteEligibilityCode = cc.getCodingFirstRep().getCode();
                break;
            }
        }

        // servicedPeriod — Date or Period polymorphic
        LocalDate servicedPeriodStart = null;
        LocalDate servicedPeriodEnd   = null;
        Type serviced = cerResponse.getServiced();
        if (serviced instanceof Period period) {
            servicedPeriodStart = toLocalDate(period.getStart());
            servicedPeriodEnd   = toLocalDate(period.getEnd());
        } else if (serviced instanceof DateType dt) {
            String dateStr = dt.getValueAsString();
            if (dateStr != null && !dateStr.isBlank()) {
                servicedPeriodStart = LocalDate.parse(dateStr);
                servicedPeriodEnd   = servicedPeriodStart;
            }
        }

        // Coverage resource from bundle (type and validity period)
        String coverageType       = null;
        LocalDate coveragePeriodStart = null;
        LocalDate coveragePeriodEnd   = null;
        Coverage fhirCoverage = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Coverage)
                .map(r -> (Coverage) r)
                .findFirst()
                .orElse(null);
        if (fhirCoverage != null) {
            if (!fhirCoverage.getType().getCoding().isEmpty()) {
                coverageType = fhirCoverage.getType().getCodingFirstRep().getCode();
            }
            Date pStart = fhirCoverage.getPeriod().getStart();
            Date pEnd   = fhirCoverage.getPeriod().getEnd();
            if (pStart != null) coveragePeriodStart = toLocalDate(pStart);
            if (pEnd   != null) coveragePeriodEnd   = toLocalDate(pEnd);
        }

        return EligibilityResponse.builder()
                .requestId(requestId)
                .bundleId(bundleId)
                .responseCode(responseCode)
                .outcome(outcome)
                .disposition(disposition)
                .inforce(inforce)
                .siteEligibilityCode(siteEligibilityCode)
                .servicedPeriodStart(servicedPeriodStart)
                .servicedPeriodEnd(servicedPeriodEnd)
                .coverageType(coverageType)
                .coveragePeriodStart(coveragePeriodStart)
                .coveragePeriodEnd(coveragePeriodEnd)
                .benefits(benefits)
                .rawResponseJson(responseJson)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private EligibilityResponse pendingResponse(String requestId, String responseJson, String bundleId) {
        return EligibilityResponse.builder()
                .requestId(requestId)
                .bundleId(bundleId)
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

    private LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
