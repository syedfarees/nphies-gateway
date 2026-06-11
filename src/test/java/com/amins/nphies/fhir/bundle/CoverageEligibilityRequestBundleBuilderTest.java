package com.amins.nphies.fhir.bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.fhir.NphiesProfiles;
import com.amins.nphies.gateway.NphiesEndpoints;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CoverageEligibilityRequestBundleBuilderTest {

    private static CoverageEligibilityRequestBundleBuilder builder;
    private static IParser parser;

    private static final String PROVIDER_LICENSE = "N-F-0000001";
    private static final String PAYER_LICENSE    = "INS-0000001";

    @BeforeAll
    static void setUpOnce() {
        FhirContext ctx = FhirContext.forR4();
        IParser p = ctx.newJsonParser().setPrettyPrint(false);
        parser  = p;
        builder = new CoverageEligibilityRequestBundleBuilder(p);
    }

    // ── Valid input produces a well-formed Bundle ─────────────────────────────

    @Test
    void build_validInput_returnsNonBlankJson() {
        String json = builder.build(minimalInput().build());
        assertThat(json).isNotBlank().contains("\"resourceType\":\"Bundle\"");
    }

    @Test
    void build_validInput_bundleTypeIsMessage() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.MESSAGE);
    }

    @Test
    void build_validInput_bundleIdMatchesRequestId() {
        String requestId = UUID.randomUUID().toString();
        Bundle bundle = parse(builder.build(minimalInput().requestId(requestId).build()));
        assertThat(bundle.getIdElement().getIdPart()).isEqualTo(requestId);
    }

    @Test
    void build_validInput_hasSixEntries() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        // MessageHeader, CoverageEligibilityRequest, Patient, Coverage, Org(provider), Org(insurer)
        assertThat(bundle.getEntry()).hasSize(6);
    }

    @Test
    void build_validInput_firstEntryIsMessageHeader() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        assertThat(bundle.getEntry().get(0).getResource())
                .isInstanceOf(MessageHeader.class);
    }

    @Test
    void build_validInput_secondEntryIsCoverageEligibilityRequest() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        assertThat(bundle.getEntry().get(1).getResource())
                .isInstanceOf(CoverageEligibilityRequest.class);
    }

    // ── MessageHeader ─────────────────────────────────────────────────────────

    @Test
    void build_messageHeader_eventCodeIsEligibilityRequest() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();

        Coding event = (Coding) hdr.getEvent();
        assertThat(event.getSystem()).isEqualTo(NphiesProfiles.CS_MESSAGE_EVENTS);
        assertThat(event.getCode()).isEqualTo(NphiesEndpoints.EVENT_ELIGIBILITY_REQUEST);
    }

    @Test
    void build_messageHeader_sourceEndpointContainsProviderLicense() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(hdr.getSource().getEndpoint()).contains(PROVIDER_LICENSE);
    }

    @Test
    void build_messageHeader_profileIsSet() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(hdr.getMeta().getProfile()).anyMatch(
                p -> p.getValue().equals(NphiesProfiles.MESSAGE_HEADER));
    }

    // ── CoverageEligibilityRequest ────────────────────────────────────────────

    @Test
    void build_cerq_statusIsActive() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        CoverageEligibilityRequest cerq = cerq(bundle);
        assertThat(cerq.getStatus())
                .isEqualTo(CoverageEligibilityRequest.EligibilityRequestStatus.ACTIVE);
    }

    @Test
    void build_cerq_defaultPurposeIsBenefits() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        CoverageEligibilityRequest cerq = cerq(bundle);
        assertThat(cerq.getPurpose()).hasSize(1);
        assertThat(cerq.getPurpose().get(0).getValueAsString()).isEqualTo("benefits");
    }

    @Test
    void build_cerq_multiplePurposesAllPresent() {
        EligibilityRequestInput input = minimalInput()
                .purposes(List.of("benefits", "discovery"))
                .build();
        CoverageEligibilityRequest cerq = cerq(parse(builder.build(input)));
        List<String> codes = cerq.getPurpose().stream()
                .map(Enumeration::getValueAsString)
                .toList();
        assertThat(codes).containsExactlyInAnyOrder("benefits", "discovery");
    }

    @Test
    void build_cerq_servicedDateIsToday_whenInputIsNull() {
        EligibilityRequestInput input = minimalInput().servicedDate(null).build();
        CoverageEligibilityRequest cerq = cerq(parse(builder.build(input)));
        assertThat(cerq.getServiced()).isNotNull();
    }

    @Test
    void build_cerq_profileIsSet() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        CoverageEligibilityRequest cerq = cerq(bundle);
        assertThat(cerq.getMeta().getProfile()).anyMatch(
                p -> p.getValue().equals(NphiesProfiles.ELIGIBILITY_REQUEST));
    }

    // ── Patient ───────────────────────────────────────────────────────────────

    @Test
    void build_patient_saudiIdUsesNationalIdSystem() {
        // Saudi national ID starts with 1
        EligibilityRequestInput input = minimalInput().patientNationalId("1234567890").build();
        Patient patient = patient(parse(builder.build(input)));
        assertThat(patient.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_NATIONAL_ID);
    }

    @Test
    void build_patient_iqamaIdUsesIqamaSystem() {
        // Iqama starts with 2
        EligibilityRequestInput input = minimalInput().patientNationalId("2987654321").build();
        Patient patient = patient(parse(builder.build(input)));
        assertThat(patient.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_IQAMA);
    }

    @Test
    void build_patient_nameIsSet() {
        Patient patient = patient(parse(builder.build(minimalInput().build())));
        assertThat(patient.getNameFirstRep().getFamily()).isEqualTo("Al-Test");
        assertThat(patient.getNameFirstRep().getGivenAsSingleString()).isEqualTo("Ahmed");
    }

    @Test
    void build_patient_genderIsSet() {
        Patient patient = patient(parse(builder.build(minimalInput().build())));
        assertThat(patient.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
    }

    // ── Coverage ──────────────────────────────────────────────────────────────

    @Test
    void build_coverage_memberIdIsSet() {
        Coverage coverage = coverage(parse(builder.build(minimalInput().build())));
        assertThat(coverage.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_MEMBER_ID);
        assertThat(coverage.getIdentifierFirstRep().getValue()).isEqualTo("MEM-001");
    }

    @Test
    void build_coverage_defaultRelationshipIsSelf() {
        Coverage coverage = coverage(parse(builder.build(minimalInput().build())));
        assertThat(coverage.getRelationship().getCodingFirstRep().getCode()).isEqualTo("self");
    }

    // ── Organisations ─────────────────────────────────────────────────────────

    @Test
    void build_providerOrg_licenseIdentifierIsSet() {
        Organization org = providerOrg(parse(builder.build(minimalInput().build())));
        assertThat(org.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PROVIDER_LICENSE);
        assertThat(org.getIdentifierFirstRep().getValue()).isEqualTo(PROVIDER_LICENSE);
    }

    @Test
    void build_insurerOrg_licenseIdentifierIsSet() {
        Organization org = insurerOrg(parse(builder.build(minimalInput().build())));
        assertThat(org.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PAYER_LICENSE);
        assertThat(org.getIdentifierFirstRep().getValue()).isEqualTo(PAYER_LICENSE);
    }

    // ── Validation guards ─────────────────────────────────────────────────────

    @Test
    void build_invalidPurpose_throwsIllegalArgument() {
        EligibilityRequestInput input = minimalInput().purposes(List.of("invalid-code")).build();
        assertThatThrownBy(() -> builder.build(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid-code");
    }

    @Test
    void build_invalidGender_throwsIllegalArgument() {
        EligibilityRequestInput input = minimalInput().patientGender("alien").build();
        assertThatThrownBy(() -> builder.build(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alien");
    }

    // ── All fullUrls are urn:uuid: ────────────────────────────────────────────

    @Test
    void build_allEntryFullUrlsUseUrnUuidScheme() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        bundle.getEntry().forEach(e ->
                assertThat(e.getFullUrl()).startsWith("urn:uuid:"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bundle parse(String json) {
        return parser.parseResource(Bundle.class, json);
    }

    private CoverageEligibilityRequest cerq(Bundle b) {
        return (CoverageEligibilityRequest) b.getEntry().get(1).getResource();
    }

    private Patient patient(Bundle b) {
        return (Patient) b.getEntry().get(2).getResource();
    }

    private Coverage coverage(Bundle b) {
        return (Coverage) b.getEntry().get(3).getResource();
    }

    private Organization providerOrg(Bundle b) {
        return (Organization) b.getEntry().get(4).getResource();
    }

    private Organization insurerOrg(Bundle b) {
        return (Organization) b.getEntry().get(5).getResource();
    }

    private EligibilityRequestInput.EligibilityRequestInputBuilder minimalInput() {
        return EligibilityRequestInput.builder()
                .requestId(UUID.randomUUID().toString())
                .patientNationalId("1234567890")
                .patientFirstName("Ahmed")
                .patientFamilyName("Al-Test")
                .patientDateOfBirth(LocalDate.of(1990, 6, 15))
                .patientGender("male")
                .memberId("MEM-001")
                .payerLicenseNumber(PAYER_LICENSE)
                .payerName("Test Insurance Co")
                .providerLicenseNumber(PROVIDER_LICENSE)
                .providerName("Test Hospital")
                .servicedDate(LocalDate.now());
    }
}
