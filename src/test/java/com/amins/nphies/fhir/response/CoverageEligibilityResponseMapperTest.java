package com.amins.nphies.fhir.response;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.RemittanceOutcome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CoverageEligibilityResponseMapperTest {

    private static CoverageEligibilityResponseMapper mapper;
    private static IParser parser;

    @BeforeAll
    static void setUpOnce() {
        FhirContext ctx = FhirContext.forR4();
        IParser p = ctx.newJsonParser().setPrettyPrint(false);
        parser = p;
        mapper = new CoverageEligibilityResponseMapper(p);
    }

    // ── Blank/null response ───────────────────────────────────────────────────

    @Test
    void map_nullResponse_returnsPending() {
        EligibilityResponse resp = mapper.map("req-1", null);
        assertThat(resp.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.PENDING);
    }

    @Test
    void map_blankResponse_returnsPending() {
        EligibilityResponse resp = mapper.map("req-1", "");
        assertThat(resp.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.PENDING);
    }

    @Test
    void map_blankResponse_inforceIsFalse() {
        EligibilityResponse resp = mapper.map("req-1", "");
        assertThat(resp.isInforce()).isFalse();
    }

    @Test
    void map_blankResponse_benefitsEmpty() {
        EligibilityResponse resp = mapper.map("req-1", "");
        assertThat(resp.getBenefits()).isEmpty();
    }

    @Test
    void map_blankResponse_requestIdEchoed() {
        EligibilityResponse resp = mapper.map("my-request-id", "");
        assertThat(resp.getRequestId()).isEqualTo("my-request-id");
    }

    // ── Bundle with no CER entry ──────────────────────────────────────────────

    @Test
    void map_bundleWithoutCer_returnsPending() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.MESSAGE);
        String json = parser.encodeResourceToString(bundle);
        EligibilityResponse resp = mapper.map("req-1", json);
        assertThat(resp.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.PENDING);
    }

    // ── Outcome mapping ───────────────────────────────────────────────────────

    @Test
    void map_outcomeComplete_mapsToComplete() {
        EligibilityResponse resp = mapper.map("req", bundleJson(cerWithOutcome("complete")));
        assertThat(resp.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.COMPLETE);
    }

    @Test
    void map_outcomeQueued_mapsToQueued() {
        EligibilityResponse resp = mapper.map("req", bundleJson(cerWithOutcome("queued")));
        assertThat(resp.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.QUEUED);
    }

    @Test
    void map_outcomeError_mapsToError() {
        EligibilityResponse resp = mapper.map("req", bundleJson(cerWithOutcome("error")));
        assertThat(resp.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.ERROR);
    }

    @Test
    void map_outcomePartial_mapsToPartial() {
        EligibilityResponse resp = mapper.map("req", bundleJson(cerWithOutcome("partial")));
        assertThat(resp.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.PARTIAL);
    }

    // ── Core fields ───────────────────────────────────────────────────────────

    @Test
    void map_disposition_isPreserved() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        cer.setDisposition("Policy is active");
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getDisposition()).isEqualTo("Policy is active");
    }

    @Test
    void map_inforceTrue_isPreserved() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        cer.addInsurance().setInforce(true);
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.isInforce()).isTrue();
    }

    @Test
    void map_inforceFalse_isPreserved() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        cer.addInsurance().setInforce(false);
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.isInforce()).isFalse();
    }

    @Test
    void map_rawResponseJson_isPreserved() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        String json = bundleJson(cer);
        EligibilityResponse resp = mapper.map("req", json);
        assertThat(resp.getRawResponseJson()).isEqualTo(json);
    }

    // ── Benefits mapping ──────────────────────────────────────────────────────

    @Test
    void map_benefits_notEmpty() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "medical", null, null, new BenefitDef("copay", money(10, "SAR"), null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits()).isNotEmpty();
    }

    @Test
    void map_benefits_categoryMapped() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "medical", null, null, new BenefitDef("copay", null, null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getCategory()).isEqualTo("medical");
    }

    @Test
    void map_benefits_networkMapped() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, null, "in", null, new BenefitDef("copay", null, null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getNetwork()).isEqualTo("in");
    }

    @Test
    void map_benefits_termMapped() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, null, null, "annual", new BenefitDef("copay", null, null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getTerm()).isEqualTo("annual");
    }

    @Test
    void map_benefits_benefitTypeMapped() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "medical", null, null, new BenefitDef("copay", null, null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getBenefitType()).isEqualTo("copay");
    }

    @Test
    void map_benefits_allowedMoney() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "medical", null, null, new BenefitDef("copay", money(500, "SAR"), null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getAllowedValue()).contains("500").contains("SAR");
    }

    @Test
    void map_benefits_usedMoney() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "medical", null, null, new BenefitDef("copay", null, money(100, "SAR")));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getUsedValue()).contains("100").contains("SAR");
    }

    @Test
    void map_benefits_allowedInt() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "visits", null, null, new BenefitDef("visits", new UnsignedIntType(20), null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getAllowedValue()).isEqualTo("20");
    }

    @Test
    void map_benefits_allowedString() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "visits", null, null, new BenefitDef("visits", new StringType("unlimited"), null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getAllowedValue()).isEqualTo("unlimited");
    }

    @Test
    void map_benefits_nullAllowed() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "medical", null, null, new BenefitDef("copay", null, null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getAllowedValue()).isNull();
    }

    // ── Flattening ────────────────────────────────────────────────────────────

    @Test
    void map_multipleBenefitsInOneItem_flattened() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItemMultipleBenefits(cer, "medical",
                new BenefitDef("copay", money(10, "SAR"), null),
                new BenefitDef("deductible", money(500, "SAR"), null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits()).hasSize(2);
    }

    @Test
    void map_multipleInsurances_allFlattened() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        addItem(cer, "medical", null, null, new BenefitDef("copay", money(10, "SAR"), null));
        addItem(cer, "dental",  null, null, new BenefitDef("copay", money(5, "SAR"),  null));
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits()).hasSize(2);
    }

    // ── CodeableConcept fallback ──────────────────────────────────────────────

    @Test
    void map_codeableConcept_textWinsOverDisplay() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        CodeableConcept cc = new CodeableConcept()
                .setText("text-value")
                .addCoding(new Coding().setDisplay("display-value").setCode("code-value"));
        addItemWithCategoryCC(cer, cc);
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getCategory()).isEqualTo("text-value");
    }

    @Test
    void map_codeableConcept_codeFallbackWhenNoDisplay() {
        CoverageEligibilityResponse cer = cerWithOutcome("complete");
        CodeableConcept cc = new CodeableConcept()
                .addCoding(new Coding().setCode("code-value"));
        addItemWithCategoryCC(cer, cc);
        EligibilityResponse resp = mapper.map("req", bundleJson(cer));
        assertThat(resp.getBenefits().get(0).getCategory()).isEqualTo("code-value");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String bundleJson(CoverageEligibilityResponse cer) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.MESSAGE);
        bundle.addEntry().setResource(cer);
        return parser.encodeResourceToString(bundle);
    }

    private CoverageEligibilityResponse cerWithOutcome(String outcomeCode) {
        CoverageEligibilityResponse cer = new CoverageEligibilityResponse();
        cer.setOutcome(RemittanceOutcome.fromCode(outcomeCode));
        return cer;
    }

    private void addItem(CoverageEligibilityResponse cer,
                         String categoryCode, String networkCode, String termCode,
                         BenefitDef benefit) {
        CoverageEligibilityResponse.InsuranceComponent insurance = cer.addInsurance();
        addItemToInsurance(insurance, categoryCode, networkCode, termCode, benefit);
    }

    private void addItemMultipleBenefits(CoverageEligibilityResponse cer,
                                         String categoryCode, BenefitDef... benefits) {
        CoverageEligibilityResponse.InsuranceComponent insurance = cer.addInsurance();
        CoverageEligibilityResponse.ItemsComponent item = insurance.addItem();
        if (categoryCode != null) item.setCategory(codeableConcept(categoryCode));
        for (BenefitDef b : benefits) {
            CoverageEligibilityResponse.BenefitComponent bc = item.addBenefit();
            if (b.type()    != null) bc.setType(codeableConcept(b.type()));
            if (b.allowed() != null) bc.setAllowed(b.allowed());
            if (b.used()    != null) bc.setUsed(b.used());
        }
    }

    private void addItemToInsurance(CoverageEligibilityResponse.InsuranceComponent insurance,
                                    String categoryCode, String networkCode, String termCode,
                                    BenefitDef benefit) {
        CoverageEligibilityResponse.ItemsComponent item = insurance.addItem();
        if (categoryCode != null) item.setCategory(codeableConcept(categoryCode));
        if (networkCode  != null) item.setNetwork(codeableConcept(networkCode));
        if (termCode     != null) item.setTerm(codeableConcept(termCode));
        if (benefit != null) {
            CoverageEligibilityResponse.BenefitComponent bc = item.addBenefit();
            if (benefit.type()    != null) bc.setType(codeableConcept(benefit.type()));
            if (benefit.allowed() != null) bc.setAllowed(benefit.allowed());
            if (benefit.used()    != null) bc.setUsed(benefit.used());
        }
    }

    private void addItemWithCategoryCC(CoverageEligibilityResponse cer, CodeableConcept category) {
        CoverageEligibilityResponse.InsuranceComponent insurance = cer.addInsurance();
        CoverageEligibilityResponse.ItemsComponent item = insurance.addItem();
        item.setCategory(category);
        item.addBenefit().setType(codeableConcept("copay"));
    }

    private CodeableConcept codeableConcept(String code) {
        return new CodeableConcept().addCoding(new Coding().setCode(code));
    }

    private Money money(int amount, String currency) {
        return new Money().setValue(BigDecimal.valueOf(amount)).setCurrency(currency);
    }

    private record BenefitDef(String type, Type allowed, Type used) {}
}
