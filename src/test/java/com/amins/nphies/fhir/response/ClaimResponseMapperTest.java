package com.amins.nphies.fhir.response;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.claim.dto.ClaimResponseDto;
import com.amins.nphies.fhir.NphiesProfiles;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimResponseMapperTest {

    private static ClaimResponseMapper mapper;
    private static IParser parser;

    @BeforeAll
    static void setUpOnce() {
        FhirContext ctx = FhirContext.forR4();
        IParser p = ctx.newJsonParser().setPrettyPrint(false);
        parser = p;
        mapper = new ClaimResponseMapper(p);
    }

    // ── Null / blank response ─────────────────────────────────────────────────

    @Test
    void map_nullResponse_returnsQueued() {
        ClaimResponseDto dto = mapper.map("claim-1", null);
        assertThat(dto.getOutcome()).isEqualTo("queued");
    }

    @Test
    void map_blankResponse_returnsQueued() {
        ClaimResponseDto dto = mapper.map("claim-1", "");
        assertThat(dto.getOutcome()).isEqualTo("queued");
    }

    // ── Bundle with no ClaimResponse resource ─────────────────────────────────

    @Test
    void map_bundleWithNoClaimResponse_returnsQueued() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.MESSAGE);
        String json = parser.encodeResourceToString(bundle);
        ClaimResponseDto dto = mapper.map("claim-1", json);
        assertThat(dto.getOutcome()).isEqualTo("queued");
    }

    // ── Outcome ───────────────────────────────────────────────────────────────

    @Test
    void map_claimResponse_outcomeIsMapped() {
        Bundle bundle = buildBundleWithClaimResponse(buildClaimResponse("complete", null, null));
        ClaimResponseDto dto = mapper.map("claim-1", parser.encodeResourceToString(bundle));
        assertThat(dto.getOutcome()).isEqualTo("complete");
    }

    // ── bundleId ──────────────────────────────────────────────────────────────

    @Test
    void map_bundleId_returnsLocalPartOnly() {
        Bundle bundle = buildBundleWithClaimResponse(buildClaimResponse("complete", null, null));
        bundle.setId("response-bundle-xyz");
        ClaimResponseDto dto = mapper.map("claim-1", parser.encodeResourceToString(bundle));
        assertThat(dto.getBundleId()).isEqualTo("response-bundle-xyz");
    }

    // ── Adjudication outcome extension ────────────────────────────────────────

    @Test
    void map_adjudicationOutcomeExtension_isExtracted() {
        ClaimResponse cr = buildClaimResponse("complete", null, null);
        CodeableConcept outcomeCC = new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem(NphiesProfiles.CS_ADJUDICATION_OUTCOME)
                        .setCode("approved"));
        cr.addExtension(new Extension(NphiesProfiles.EXT_ADJUDICATION_OUTCOME, outcomeCC));

        Bundle bundle = buildBundleWithClaimResponse(cr);
        ClaimResponseDto dto = mapper.map("claim-1", parser.encodeResourceToString(bundle));
        assertThat(dto.getAdjudicationOutcomeCode()).isEqualTo("approved");
    }

    @Test
    void map_noAdjudicationOutcomeExtension_isNull() {
        ClaimResponse cr = buildClaimResponse("complete", null, null);
        Bundle bundle = buildBundleWithClaimResponse(cr);
        ClaimResponseDto dto = mapper.map("claim-1", parser.encodeResourceToString(bundle));
        assertThat(dto.getAdjudicationOutcomeCode()).isNull();
    }

    // ── Totals ────────────────────────────────────────────────────────────────

    @Test
    void map_totals_benefitAndSubmittedAreMapped() {
        ClaimResponse cr = buildClaimResponse("complete", null, null);
        ClaimResponse.TotalComponent benefit = new ClaimResponse.TotalComponent();
        benefit.setCategory(new CodeableConcept().addCoding(new Coding().setCode("benefit")));
        benefit.setAmount(new Money().setValue(BigDecimal.valueOf(1500)).setCurrency("SAR"));
        ClaimResponse.TotalComponent submitted = new ClaimResponse.TotalComponent();
        submitted.setCategory(new CodeableConcept().addCoding(new Coding().setCode("submitted")));
        submitted.setAmount(new Money().setValue(BigDecimal.valueOf(2000)).setCurrency("SAR"));
        cr.setTotal(List.of(benefit, submitted));

        Bundle bundle = buildBundleWithClaimResponse(cr);
        ClaimResponseDto dto = mapper.map("claim-1", parser.encodeResourceToString(bundle));
        assertThat(dto.getTotalBenefit()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(dto.getTotalSubmitted()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @Test
    void map_payment_amountAndDateAreMapped() {
        ClaimResponse.PaymentComponent payment = new ClaimResponse.PaymentComponent();
        payment.setAmount(new Money().setValue(BigDecimal.valueOf(1500)).setCurrency("SAR"));
        payment.setDateElement(new DateType("2025-03-15"));
        ClaimResponse cr = buildClaimResponse("complete", null, null);
        cr.setPayment(payment);

        Bundle bundle = buildBundleWithClaimResponse(cr);
        ClaimResponseDto dto = mapper.map("claim-1", parser.encodeResourceToString(bundle));
        assertThat(dto.getPaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(dto.getPaymentDate()).isEqualTo(LocalDate.of(2025, 3, 15));
        assertThat(dto.getCurrency()).isEqualTo("SAR");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClaimResponse buildClaimResponse(String outcome, String disposition, String bundleId) {
        ClaimResponse cr = new ClaimResponse();
        cr.setOutcome(ClaimResponse.RemittanceOutcome.fromCode(outcome));
        if (disposition != null) cr.setDisposition(disposition);
        return cr;
    }

    private Bundle buildBundleWithClaimResponse(ClaimResponse cr) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.MESSAGE);
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setResource(cr);
        bundle.addEntry(entry);
        return bundle;
    }
}
