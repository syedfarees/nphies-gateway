package com.amins.nphies.fhir.response;

import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.claim.dto.ClaimResponseDto;
import com.amins.nphies.fhir.NphiesProfiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimResponseMapper {

    private final IParser fhirJsonParser;

    public ClaimResponseDto map(String claimId, String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            log.warn("Empty response from NPHIES for claim: {}", claimId);
            return ClaimResponseDto.builder()
                    .outcome("queued")
                    .receivedAt(OffsetDateTime.now())
                    .currency("SAR")
                    .build();
        }

        try {
            Bundle bundle = fhirJsonParser.parseResource(Bundle.class, responseJson);
            String responseBundleId = bundle.hasId() ? bundle.getIdElement().getIdPart() : null;

            ClaimResponse claimResponse = bundle.getEntry().stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(r -> r instanceof ClaimResponse)
                    .map(r -> (ClaimResponse) r)
                    .findFirst()
                    .orElse(null);

            if (claimResponse == null) {
                log.warn("No ClaimResponse resource found in bundle for claim: {}", claimId);
                return ClaimResponseDto.builder()
                        .outcome("queued")
                        .receivedAt(OffsetDateTime.now())
                        .currency("SAR")
                        .build();
            }

            String outcome = claimResponse.getOutcome() != null
                    ? claimResponse.getOutcome().toCode() : "queued";

            String disposition = claimResponse.getDisposition();

            String adjudicationOutcomeCode = null;
            for (Extension ext : claimResponse.getExtension()) {
                if (NphiesProfiles.EXT_ADJUDICATION_OUTCOME.equals(ext.getUrl())
                        && ext.getValue() instanceof CodeableConcept cc
                        && !cc.getCoding().isEmpty()) {
                    adjudicationOutcomeCode = cc.getCodingFirstRep().getCode();
                    break;
                }
            }

            BigDecimal totalBenefit = null;
            BigDecimal totalSubmitted = null;
            if (claimResponse.hasTotal()) {
                for (ClaimResponse.TotalComponent total : claimResponse.getTotal()) {
                    String cat = total.getCategory().getCodingFirstRep().getCode();
                    if ("benefit".equals(cat) && total.hasAmount()) {
                        totalBenefit = total.getAmount().getValue();
                    } else if ("submitted".equals(cat) && total.hasAmount()) {
                        totalSubmitted = total.getAmount().getValue();
                    }
                }
            }

            BigDecimal paymentAmount = null;
            LocalDate paymentDate = null;
            String currency = "SAR";
            if (claimResponse.hasPayment()) {
                ClaimResponse.PaymentComponent payment = claimResponse.getPayment();
                if (payment.hasAmount()) {
                    paymentAmount = payment.getAmount().getValue();
                    currency = payment.getAmount().getCurrency() != null
                            ? payment.getAmount().getCurrency() : "SAR";
                }
                if (payment.hasDate()) {
                    paymentDate = LocalDate.parse(payment.getDateElement().getValueAsString());
                }
            }

            return ClaimResponseDto.builder()
                    .bundleId(responseBundleId)
                    .outcome(outcome)
                    .disposition(disposition)
                    .adjudicationOutcomeCode(adjudicationOutcomeCode)
                    .totalBenefit(totalBenefit)
                    .totalSubmitted(totalSubmitted)
                    .paymentAmount(paymentAmount)
                    .paymentDate(paymentDate)
                    .currency(currency)
                    .receivedAt(OffsetDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse NPHIES ClaimResponse for claim: {}", claimId, e);
            return ClaimResponseDto.builder()
                    .outcome("error")
                    .disposition("Failed to parse response: " + e.getMessage())
                    .receivedAt(OffsetDateTime.now())
                    .currency("SAR")
                    .build();
        }
    }
}
