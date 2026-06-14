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
    private static final String BASE_URL         = "http://provider.com";

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
    void build_validInput_bundleProfileIsVersioned() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        assertThat(bundle.getMeta().getProfile())
                .anyMatch(p -> p.getValue().equals(NphiesProfiles.versioned(NphiesProfiles.BUNDLE)));
    }

    @Test
    void build_validInput_hasSixEntries() {
        Bundle bundle = parse(builder.build(minimalInput().build()));
        assertThat(bundle.getEntry()).hasSize(6);
    }

    // ── Entry order ───────────────────────────────────────────────────────────

    @Test
    void build_entryOrder_messageHeaderFirst() {
        assertThat(parse(builder.build(minimalInput().build())).getEntry().get(0).getResource())
                .isInstanceOf(MessageHeader.class);
    }

    @Test
    void build_entryOrder_cerqSecond() {
        assertThat(parse(builder.build(minimalInput().build())).getEntry().get(1).getResource())
                .isInstanceOf(CoverageEligibilityRequest.class);
    }

    @Test
    void build_entryOrder_coverageThird() {
        assertThat(parse(builder.build(minimalInput().build())).getEntry().get(2).getResource())
                .isInstanceOf(Coverage.class);
    }

    @Test
    void build_entryOrder_providerOrgFourth() {
        Organization org = (Organization) parse(builder.build(minimalInput().build())).getEntry().get(3).getResource();
        assertThat(org.getIdentifierFirstRep().getSystem()).isEqualTo(NphiesProfiles.SYSTEM_PROVIDER_LICENSE);
    }

    @Test
    void build_entryOrder_patientFifth() {
        assertThat(parse(builder.build(minimalInput().build())).getEntry().get(4).getResource())
                .isInstanceOf(Patient.class);
    }

    @Test
    void build_entryOrder_insurerOrgSixth() {
        Organization org = (Organization) parse(builder.build(minimalInput().build())).getEntry().get(5).getResource();
        assertThat(org.getIdentifierFirstRep().getSystem()).isEqualTo(NphiesProfiles.SYSTEM_PAYER_LICENSE);
    }

    // ── fullUrl scheme ────────────────────────────────────────────────────────

    @Test
    void build_messageHeader_fullUrlIsUrnUuid() {
        assertThat(parse(builder.build(minimalInput().build())).getEntry().get(0).getFullUrl())
                .startsWith("urn:uuid:");
    }

    @Test
    void build_otherEntries_fullUrlsUseProviderBaseUrl() {
        parse(builder.build(minimalInput().build())).getEntry().stream().skip(1)
                .forEach(e -> assertThat(e.getFullUrl()).startsWith(BASE_URL));
    }

    // ── MessageHeader ─────────────────────────────────────────────────────────

    @Test
    void build_messageHeader_eventCodeIsEligibilityRequest() {
        MessageHeader hdr = msgHeader(parse(builder.build(minimalInput().build())));
        Coding event = (Coding) hdr.getEvent();
        assertThat(event.getSystem()).isEqualTo(NphiesProfiles.CS_MESSAGE_EVENTS);
        assertThat(event.getCode()).isEqualTo(NphiesEndpoints.EVENT_ELIGIBILITY_REQUEST);
    }

    @Test
    void build_messageHeader_sourceEndpointIsProviderBaseUrl() {
        MessageHeader hdr = msgHeader(parse(builder.build(minimalInput().build())));
        assertThat(hdr.getSource().getEndpoint()).isEqualTo(BASE_URL);
    }

    @Test
    void build_messageHeader_hasSenderWithProviderLicense() {
        MessageHeader hdr = msgHeader(parse(builder.build(minimalInput().build())));
        assertThat(hdr.getSender().getType()).isEqualTo("Organization");
        assertThat(hdr.getSender().getIdentifier().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PROVIDER_LICENSE);
        assertThat(hdr.getSender().getIdentifier().getValue()).isEqualTo(PROVIDER_LICENSE);
    }

    @Test
    void build_messageHeader_destinationHasNphiesEndpointAndReceiver() {
        MessageHeader hdr = msgHeader(parse(builder.build(minimalInput().build())));
        MessageHeader.MessageDestinationComponent dest = hdr.getDestinationFirstRep();
        assertThat(dest.getEndpoint()).isEqualTo(NphiesEndpoints.NPHIES_DESTINATION_ENDPOINT);
        assertThat(dest.getReceiver().getType()).isEqualTo("Organization");
        assertThat(dest.getReceiver().getIdentifier().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PAYER_LICENSE);
        assertThat(dest.getReceiver().getIdentifier().getValue()).isEqualTo(PAYER_LICENSE);
    }

    @Test
    void build_messageHeader_profileIsVersioned() {
        MessageHeader hdr = msgHeader(parse(builder.build(minimalInput().build())));
        assertThat(hdr.getMeta().getProfile())
                .anyMatch(p -> p.getValue().equals(NphiesProfiles.versioned(NphiesProfiles.MESSAGE_HEADER)));
    }

    // ── CoverageEligibilityRequest ────────────────────────────────────────────

    @Test
    void build_cerq_statusIsActive() {
        assertThat(cerq(parse(builder.build(minimalInput().build()))).getStatus())
                .isEqualTo(CoverageEligibilityRequest.EligibilityRequestStatus.ACTIVE);
    }

    @Test
    void build_cerq_defaultPurposeIsBenefits() {
        CoverageEligibilityRequest cerq = cerq(parse(builder.build(minimalInput().build())));
        assertThat(cerq.getPurpose()).hasSize(1);
        assertThat(cerq.getPurpose().get(0).getValueAsString()).isEqualTo("benefits");
    }

    @Test
    void build_cerq_multiplePurposesAllPresent() {
        EligibilityRequestInput input = minimalInput().purposes(List.of("benefits", "discovery")).build();
        List<String> codes = cerq(parse(builder.build(input))).getPurpose().stream()
                .map(Enumeration::getValueAsString).toList();
        assertThat(codes).containsExactlyInAnyOrder("benefits", "discovery");
    }

    @Test
    void build_cerq_hasIdentifier() {
        CoverageEligibilityRequest cerq = cerq(parse(builder.build(minimalInput().build())));
        assertThat(cerq.getIdentifier()).isNotEmpty();
        assertThat(cerq.getIdentifierFirstRep().getValue()).isNotBlank();
    }

    @Test
    void build_cerq_hasServicedPeriod() {
        CoverageEligibilityRequest cerq = cerq(parse(builder.build(minimalInput().build())));
        assertThat(cerq.getServiced()).isInstanceOf(Period.class);
        Period p = (Period) cerq.getServiced();
        assertThat(p.getStart()).isNotNull();
        assertThat(p.getEnd()).isNotNull();
    }

    @Test
    void build_cerq_servicedPeriodDefaultsToToday_whenInputIsNull() {
        EligibilityRequestInput input = minimalInput().servicedDate(null).build();
        assertThat(cerq(parse(builder.build(input))).getServiced()).isNotNull();
    }

    @Test
    void build_cerq_hasPriorityStat() {
        CoverageEligibilityRequest cerq = cerq(parse(builder.build(minimalInput().build())));
        assertThat(cerq.getPriority().getCodingFirstRep().getCode()).isEqualTo("stat");
        assertThat(cerq.getPriority().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_PROCESS_PRIORITY);
    }

    @Test
    void build_cerq_profileIsVersioned() {
        assertThat(cerq(parse(builder.build(minimalInput().build()))).getMeta().getProfile())
                .anyMatch(p -> p.getValue().equals(
                        NphiesProfiles.versioned(NphiesProfiles.ELIGIBILITY_REQUEST)));
    }

    // ── Patient ───────────────────────────────────────────────────────────────

    @Test
    void build_patient_saudiIdUsesNationalIdSystem() {
        EligibilityRequestInput input = minimalInput().patientNationalId("1234567890").build();
        assertThat(patient(parse(builder.build(input))).getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_NATIONAL_ID);
    }

    @Test
    void build_patient_iqamaIdUsesIqamaSystem() {
        EligibilityRequestInput input = minimalInput().patientNationalId("2987654321").build();
        assertThat(patient(parse(builder.build(input))).getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_IQAMA);
    }

    @Test
    void build_patient_nameIsSet() {
        Patient patient = patient(parse(builder.build(minimalInput().build())));
        assertThat(patient.getNameFirstRep().getFamily()).isEqualTo("Al-Test");
        assertThat(patient.getNameFirstRep().getGivenAsSingleString()).isEqualTo("Ahmed");
    }

    @Test
    void build_patient_nameUseIsOfficial() {
        assertThat(patient(parse(builder.build(minimalInput().build()))).getNameFirstRep().getUse())
                .isEqualTo(HumanName.NameUse.OFFICIAL);
    }

    @Test
    void build_patient_nameTextIsSet() {
        assertThat(patient(parse(builder.build(minimalInput().build()))).getNameFirstRep().getText())
                .isNotBlank();
    }

    @Test
    void build_patient_genderIsSet() {
        assertThat(patient(parse(builder.build(minimalInput().build()))).getGender())
                .isEqualTo(Enumerations.AdministrativeGender.MALE);
    }

    @Test
    void build_patient_genderHasKsaExtension() {
        Patient patient = patient(parse(builder.build(minimalInput().build())));
        boolean hasExt = patient.getGenderElement().getExtension().stream()
                .anyMatch(e -> NphiesProfiles.EXT_KSA_ADMIN_GENDER.equals(e.getUrl()));
        assertThat(hasExt).isTrue();
    }

    @Test
    void build_patient_hasManagingOrganization() {
        assertThat(patient(parse(builder.build(minimalInput().build())))
                .getManagingOrganization().getReference()).isNotBlank();
    }

    @Test
    void build_patient_isActive() {
        assertThat(patient(parse(builder.build(minimalInput().build()))).getActive()).isTrue();
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
        assertThat(coverage(parse(builder.build(minimalInput().build())))
                .getRelationship().getCodingFirstRep().getCode()).isEqualTo("self");
    }

    @Test
    void build_coverage_hasTypeCodingEhcpol() {
        Coverage coverage = coverage(parse(builder.build(minimalInput().build())));
        assertThat(coverage.getType().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_COVERAGE_TYPE);
        assertThat(coverage.getType().getCodingFirstRep().getCode()).isEqualTo("EHCPOL");
    }

    @Test
    void build_coverage_hasPolicyHolder() {
        assertThat(coverage(parse(builder.build(minimalInput().build())))
                .getPolicyHolder().getReference()).isNotBlank();
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
    void build_providerOrg_identifierUseIsOfficial() {
        assertThat(providerOrg(parse(builder.build(minimalInput().build())))
                .getIdentifierFirstRep().getUse()).isEqualTo(Identifier.IdentifierUse.OFFICIAL);
    }

    @Test
    void build_providerOrg_hasTypeCodeProv() {
        Organization org = providerOrg(parse(builder.build(minimalInput().build())));
        assertThat(org.getTypeFirstRep().getCodingFirstRep().getCode()).isEqualTo("prov");
        assertThat(org.getTypeFirstRep().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_ORG_TYPE);
    }

    @Test
    void build_insurerOrg_licenseIdentifierIsSet() {
        Organization org = insurerOrg(parse(builder.build(minimalInput().build())));
        assertThat(org.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PAYER_LICENSE);
        assertThat(org.getIdentifierFirstRep().getValue()).isEqualTo(PAYER_LICENSE);
    }

    @Test
    void build_insurerOrg_hasTypeCodeIns() {
        Organization org = insurerOrg(parse(builder.build(minimalInput().build())));
        assertThat(org.getTypeFirstRep().getCodingFirstRep().getCode()).isEqualTo("ins");
        assertThat(org.getTypeFirstRep().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_ORG_TYPE);
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bundle parse(String json) { return parser.parseResource(Bundle.class, json); }

    private MessageHeader             msgHeader(Bundle b) { return (MessageHeader)             b.getEntry().get(0).getResource(); }
    private CoverageEligibilityRequest cerq(Bundle b)     { return (CoverageEligibilityRequest) b.getEntry().get(1).getResource(); }
    private Coverage                  coverage(Bundle b)  { return (Coverage)                  b.getEntry().get(2).getResource(); }
    private Organization              providerOrg(Bundle b){ return (Organization)              b.getEntry().get(3).getResource(); }
    private Patient                   patient(Bundle b)    { return (Patient)                   b.getEntry().get(4).getResource(); }
    private Organization              insurerOrg(Bundle b) { return (Organization)              b.getEntry().get(5).getResource(); }

    private EligibilityRequestInput.EligibilityRequestInputBuilder minimalInput() {
        return EligibilityRequestInput.builder()
                .requestId(UUID.randomUUID().toString())
                .providerBaseUrl(BASE_URL)
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
