package com.amins.nphies.fhir.bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.fhir.NphiesProfiles;
import com.amins.nphies.gateway.NphiesEndpoints;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimBundleBuilderTest {

    private static final String PROVIDER_LICENSE = "N-F-0000001";
    private static final String PAYER_LICENSE     = "INS-0000001";
    private static final String BASE_URL          = "http://provider.com";

    private ClaimBundleBuilder builder;
    private IParser parser;

    @BeforeEach
    void setUp() {
        FhirContext ctx = FhirContext.forR4();
        parser  = ctx.newJsonParser().setPrettyPrint(false);
        builder = new ClaimBundleBuilder(parser);
    }

    private ClaimBundleInput minimalInput() {
        return ClaimBundleInput.builder()
                .requestId("bundle-001")
                .providerBaseUrl(BASE_URL)
                .useType("claim")
                .claimType("institutional")
                .priority("normal")
                .patientNationalId("1234567890")
                .patientFirstName("Ahmed")
                .patientFamilyName("Al-Test")
                .patientDob(LocalDate.of(1990, 6, 15))
                .patientGender("male")
                .memberId("MEM-001")
                .payerLicenseNo(PAYER_LICENSE)
                .payerName("Test Insurance")
                .providerLicenseNo(PROVIDER_LICENSE)
                .providerName("Test Hospital")
                .billablePeriodStart(LocalDate.of(2025, 1, 1))
                .billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .build();
    }

    private Bundle parse(ClaimBundleInput input) {
        String json = builder.build(input);
        return parser.parseResource(Bundle.class, json);
    }

    // ── Bundle ────────────────────────────────────────────────────────────────

    @Test
    void build_bundleProfile_hasVersionSuffix() {
        Bundle bundle = parse(minimalInput());
        assertThat(bundle.getMeta().getProfile())
                .anyMatch(c -> c.getValue().equals(NphiesProfiles.versioned(NphiesProfiles.BUNDLE)));
    }

    @Test
    void build_bundleType_isMessage() {
        Bundle bundle = parse(minimalInput());
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.MESSAGE);
    }

    // ── MessageHeader ────────────────────────────────────────────────────────

    @Test
    void build_messageHeader_fullUrlIsUrnUuid() {
        Bundle bundle = parse(minimalInput());
        String fullUrl = bundle.getEntry().get(0).getFullUrl();
        assertThat(fullUrl).startsWith("urn:uuid:");
    }

    @Test
    void build_messageHeader_profileHasVersionSuffix() {
        Bundle bundle = parse(minimalInput());
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(hdr.getMeta().getProfile())
                .anyMatch(c -> c.getValue().equals(NphiesProfiles.versioned(NphiesProfiles.MESSAGE_HEADER)));
    }

    @Test
    void build_messageHeader_eventCodeIsClaimRequest() {
        Bundle bundle = parse(minimalInput());
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(((Coding) hdr.getEvent()).getCode()).isEqualTo(NphiesEndpoints.EVENT_CLAIM_REQUEST);
    }

    @Test
    void build_messageHeader_eventCodeIsPriorAuthForPreauthorization() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("pa-001").providerBaseUrl(BASE_URL)
                .useType("preauthorization").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .build();
        Bundle bundle = parse(input);
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(((Coding) hdr.getEvent()).getCode()).isEqualTo(NphiesEndpoints.EVENT_PRIORAUTH_REQUEST);
    }

    @Test
    void build_messageHeader_senderIsProviderByLicense() {
        Bundle bundle = parse(minimalInput());
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(hdr.getSender().getType()).isEqualTo("Organization");
        assertThat(hdr.getSender().getIdentifier().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PROVIDER_LICENSE);
        assertThat(hdr.getSender().getIdentifier().getValue()).isEqualTo(PROVIDER_LICENSE);
    }

    @Test
    void build_messageHeader_destinationEndpointIsNphiesFixed() {
        Bundle bundle = parse(minimalInput());
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(hdr.getDestinationFirstRep().getEndpoint())
                .isEqualTo(NphiesEndpoints.NPHIES_DESTINATION_ENDPOINT);
    }

    @Test
    void build_messageHeader_receiverIsPayerByLicense() {
        Bundle bundle = parse(minimalInput());
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        Reference receiver = hdr.getDestinationFirstRep().getReceiver();
        assertThat(receiver.getType()).isEqualTo("Organization");
        assertThat(receiver.getIdentifier().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PAYER_LICENSE);
        assertThat(receiver.getIdentifier().getValue()).isEqualTo(PAYER_LICENSE);
    }

    @Test
    void build_messageHeader_sourceEndpointIsProviderBaseUrl() {
        Bundle bundle = parse(minimalInput());
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(hdr.getSource().getEndpoint()).isEqualTo(BASE_URL);
    }

    @Test
    void build_messageHeader_focusUsesHttpClaimUrl() {
        Bundle bundle = parse(minimalInput());
        MessageHeader hdr = (MessageHeader) bundle.getEntry().get(0).getResource();
        assertThat(hdr.getFocusFirstRep().getReference()).startsWith(BASE_URL + "/Claim/");
    }

    // ── Entry order and fullUrls ─────────────────────────────────────────────

    @Test
    void build_claimEntry_isAtIndex1WithHttpUrl() {
        Bundle bundle = parse(minimalInput());
        Bundle.BundleEntryComponent entry = bundle.getEntry().get(1);
        assertThat(entry.getFullUrl()).startsWith(BASE_URL + "/Claim/");
        assertThat(entry.getResource()).isInstanceOf(Claim.class);
    }

    @Test
    void build_providerOrgEntry_isAtIndex2() {
        Bundle bundle = parse(minimalInput());
        assertThat(bundle.getEntry().get(2).getResource()).isInstanceOf(Organization.class);
        Organization org = (Organization) bundle.getEntry().get(2).getResource();
        assertThat(org.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PROVIDER_LICENSE);
    }

    @Test
    void build_patientEntry_isAtIndex3() {
        Bundle bundle = parse(minimalInput());
        assertThat(bundle.getEntry().get(3).getResource()).isInstanceOf(Patient.class);
    }

    @Test
    void build_insurerOrgEntry_isAtIndex4() {
        Bundle bundle = parse(minimalInput());
        assertThat(bundle.getEntry().get(4).getResource()).isInstanceOf(Organization.class);
        Organization org = (Organization) bundle.getEntry().get(4).getResource();
        assertThat(org.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PAYER_LICENSE);
    }

    @Test
    void build_coverageEntry_isAtIndex5() {
        Bundle bundle = parse(minimalInput());
        assertThat(bundle.getEntry().get(5).getResource()).isInstanceOf(Coverage.class);
    }

    // ── Claim resource ────────────────────────────────────────────────────────

    @Test
    void build_claim_profileIsInstitutionalClaim() {
        Bundle bundle = parse(minimalInput());
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getMeta().getProfile())
                .anyMatch(c -> c.getValue()
                        .equals(NphiesProfiles.versioned(NphiesProfiles.INSTITUTIONAL_CLAIM)));
    }

    @Test
    void build_claim_hasIdentifier() {
        Bundle bundle = parse(minimalInput());
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getIdentifier()).isNotEmpty();
        assertThat(claim.getIdentifierFirstRep().getValue()).isNotBlank();
    }

    @Test
    void build_claim_subType_isSetWhenProvided() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").claimSubType("emr").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getSubType().getCodingFirstRep().getCode()).isEqualTo("emr");
        assertThat(claim.getSubType().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_CLAIM_SUBTYPE);
    }

    @Test
    void build_claim_payeeTypeIsProvider() {
        Bundle bundle = parse(minimalInput());
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getPayee().getType().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_PAYEE_TYPE);
        assertThat(claim.getPayee().getType().getCodingFirstRep().getCode()).isEqualTo("provider");
    }

    @Test
    void build_claim_insuranceHasIdentifierAndCoverageRef() {
        Bundle bundle = parse(minimalInput());
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        Claim.InsuranceComponent ins = claim.getInsuranceFirstRep();
        assertThat(ins.getIdentifier().getValue()).isNotBlank();
        assertThat(ins.getCoverage().getReference()).startsWith(BASE_URL + "/Coverage/");
    }

    // ── Care team and PractitionerRole ────────────────────────────────────────

    @Test
    void build_careTeam_referencesPractitionerRoleUrl() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .careTeam(List.of(ClaimBundleInput.CareTeamMember.builder()
                        .sequence(1).practitionerLicense("PRAC-001")
                        .firstName("Dr").familyName("Smith").roleCode("doctor")
                        .qualification("08.22").build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getCareTeamFirstRep().getProvider().getReference())
                .startsWith(BASE_URL + "/PractitionerRole/");
    }

    @Test
    void build_careTeam_qualificationUsesPracticeCodesSystem() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .careTeam(List.of(ClaimBundleInput.CareTeamMember.builder()
                        .sequence(1).practitionerLicense("PRAC-001")
                        .firstName("Dr").familyName("Smith").roleCode("doctor")
                        .qualification("01.00").build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getCareTeamFirstRep().getQualification().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_PRACTICE_CODES);
        assertThat(claim.getCareTeamFirstRep().getQualification().getCodingFirstRep().getCode())
                .isEqualTo("01.00");
    }

    @Test
    void build_practitionerRole_structureIsCorrect() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .careTeam(List.of(ClaimBundleInput.CareTeamMember.builder()
                        .sequence(1).practitionerLicense("PRAC-06-362")
                        .firstName("Dr").familyName("Smith").roleCode("doctor")
                        .qualification("08.22").build()))
                .build();
        Bundle bundle = parse(input);
        PractitionerRole role = (PractitionerRole) bundle.getEntry().get(6).getResource();

        assertThat(role.getMeta().getProfile())
                .anyMatch(c -> c.getValue().equals(NphiesProfiles.versioned(NphiesProfiles.PRACTITIONER_ROLE)));
        assertThat(role.getActive()).isTrue();
        assertThat(role.getIdentifierFirstRep().getSystem()).isEqualTo(NphiesProfiles.CS_PRACTITIONER_ROLE);
        assertThat(role.getIdentifierFirstRep().getValue()).isEqualTo("doctor");
        assertThat(role.getPractitioner().getType()).isEqualTo("Practitioner");
        assertThat(role.getPractitioner().getIdentifier().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PRACTITIONER_LICENSE);
        assertThat(role.getPractitioner().getIdentifier().getValue()).isEqualTo("PRAC-06-362");
        assertThat(role.getOrganization().getReference()).startsWith(BASE_URL + "/Organization/");
        assertThat(role.getCodeFirstRep().getCodingFirstRep().getCode()).isEqualTo("doctor");
        assertThat(role.getSpecialtyFirstRep().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_PRACTICE_CODES);
        assertThat(role.getSpecialtyFirstRep().getCodingFirstRep().getCode()).isEqualTo("08.22");
    }

    // ── Diagnosis ─────────────────────────────────────────────────────────────

    @Test
    void build_diagnosis_onAdmissionIsSetWhenProvided() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .diagnoses(List.of(ClaimBundleInput.DiagnosisEntry.builder()
                        .sequence(1).icd10Code("A01.1").onAdmissionCode("y").build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        Claim.DiagnosisComponent dc = claim.getDiagnosisFirstRep();
        assertThat(dc.getOnAdmission().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_DIAGNOSIS_ON_ADMISSION);
        assertThat(dc.getOnAdmission().getCodingFirstRep().getCode()).isEqualTo("y");
    }

    // ── Organizations ─────────────────────────────────────────────────────────

    @Test
    void build_providerOrg_hasTypeCodingProv() {
        Bundle bundle = parse(minimalInput());
        Organization org = (Organization) bundle.getEntry().get(2).getResource();
        assertThat(org.getTypeFirstRep().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_ORG_TYPE);
        assertThat(org.getTypeFirstRep().getCodingFirstRep().getCode()).isEqualTo("prov");
    }

    @Test
    void build_insurerOrg_hasTypeCodingIns() {
        Bundle bundle = parse(minimalInput());
        Organization org = (Organization) bundle.getEntry().get(4).getResource();
        assertThat(org.getTypeFirstRep().getCodingFirstRep().getCode()).isEqualTo("ins");
    }

    @Test
    void build_providerOrg_identifierUseIsOfficial() {
        Bundle bundle = parse(minimalInput());
        Organization org = (Organization) bundle.getEntry().get(2).getResource();
        assertThat(org.getIdentifierFirstRep().getUse()).isEqualTo(Identifier.IdentifierUse.OFFICIAL);
    }

    @Test
    void build_insurerOrg_identifierUseIsOfficial() {
        Bundle bundle = parse(minimalInput());
        Organization org = (Organization) bundle.getEntry().get(4).getResource();
        assertThat(org.getIdentifierFirstRep().getUse()).isEqualTo(Identifier.IdentifierUse.OFFICIAL);
    }

    // ── Patient ───────────────────────────────────────────────────────────────

    @Test
    void build_patient_isActive() {
        Bundle bundle = parse(minimalInput());
        Patient patient = (Patient) bundle.getEntry().get(3).getResource();
        assertThat(patient.getActive()).isTrue();
    }

    @Test
    void build_patient_nameUseIsOfficial() {
        Bundle bundle = parse(minimalInput());
        Patient patient = (Patient) bundle.getEntry().get(3).getResource();
        assertThat(patient.getNameFirstRep().getUse()).isEqualTo(HumanName.NameUse.OFFICIAL);
    }

    @Test
    void build_patient_hasKsaGenderExtension() {
        Bundle bundle = parse(minimalInput());
        Patient patient = (Patient) bundle.getEntry().get(3).getResource();
        boolean hasExt = patient.getGenderElement().getExtension().stream()
                .anyMatch(e -> e.getUrl().equals(NphiesProfiles.EXT_KSA_ADMIN_GENDER));
        assertThat(hasExt).isTrue();
    }

    @Test
    void build_patient_managingOrganizationRefsProviderOrg() {
        Bundle bundle = parse(minimalInput());
        Patient patient = (Patient) bundle.getEntry().get(3).getResource();
        assertThat(patient.getManagingOrganization().getReference())
                .startsWith(BASE_URL + "/Organization/");
    }

    @Test
    void build_patient_saudiIdUsesNationalIdSystem() {
        Bundle bundle = parse(minimalInput()); // national id starts with 1
        Patient patient = (Patient) bundle.getEntry().get(3).getResource();
        assertThat(patient.getIdentifierFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_NATIONAL_ID);
        assertThat(patient.getIdentifierFirstRep().getType().getCodingFirstRep().getCode())
                .isEqualTo("NI");
    }

    // ── Coverage ─────────────────────────────────────────────────────────────

    @Test
    void build_coverage_hasTypeEhcpol() {
        Bundle bundle = parse(minimalInput());
        Coverage coverage = (Coverage) bundle.getEntry().get(5).getResource();
        assertThat(coverage.getType().getCodingFirstRep().getSystem())
                .isEqualTo(NphiesProfiles.CS_COVERAGE_TYPE);
        assertThat(coverage.getType().getCodingFirstRep().getCode()).isEqualTo("EHCPOL");
    }

    @Test
    void build_coverage_profileHasVersionSuffix() {
        Bundle bundle = parse(minimalInput());
        Coverage coverage = (Coverage) bundle.getEntry().get(5).getResource();
        assertThat(coverage.getMeta().getProfile())
                .anyMatch(c -> c.getValue().equals(NphiesProfiles.versioned(NphiesProfiles.COVERAGE)));
    }

    @Test
    void build_coverage_periodIsSetWhenProvided() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .coveragePeriodStart(LocalDate.of(2025, 1, 1))
                .coveragePeriodEnd(LocalDate.of(2025, 12, 31))
                .build();
        Bundle bundle = parse(input);
        Coverage coverage = (Coverage) bundle.getEntry().get(5).getResource();
        assertThat(coverage.getPeriod()).isNotNull();
        assertThat(coverage.getPeriod().getStart()).isNotNull();
        assertThat(coverage.getPeriod().getEnd()).isNotNull();
    }

    // ── Item extensions ───────────────────────────────────────────────────────

    @Test
    void build_item_taxExtensionIsSet() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .items(List.of(ClaimBundleInput.ClaimItemEntry.builder()
                        .sequence(1).productCode("30394-01-00")
                        .servicedDate(LocalDate.of(2025, 1, 15))
                        .unitPrice(BigDecimal.valueOf(100)).net(BigDecimal.valueOf(110))
                        .taxAmount(BigDecimal.valueOf(10))
                        .build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        boolean hasTax = claim.getItemFirstRep().getExtension().stream()
                .anyMatch(e -> e.getUrl().equals(NphiesProfiles.EXT_TAX));
        assertThat(hasTax).isTrue();
    }

    @Test
    void build_item_packageExtensionIsSetWhenTrue() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .items(List.of(ClaimBundleInput.ClaimItemEntry.builder()
                        .sequence(1).productCode("PKG-01")
                        .servicedDate(LocalDate.of(2025, 1, 15))
                        .unitPrice(BigDecimal.valueOf(500)).net(BigDecimal.valueOf(500))
                        .isPackage(true)
                        .build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        boolean hasPkg = claim.getItemFirstRep().getExtension().stream()
                .anyMatch(e -> e.getUrl().equals(NphiesProfiles.EXT_PACKAGE));
        assertThat(hasPkg).isTrue();
    }

    // ── Supporting info ───────────────────────────────────────────────────────

    @Test
    void build_supportingInfo_isAddedToClaimWhenProvided() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .supportingInfo(List.of(
                        ClaimBundleInput.SupportingInfoEntry.builder()
                                .sequence(1).categoryCode("temperature")
                                .quantityValue(BigDecimal.valueOf(37)).quantityUnit("[degF]").build(),
                        ClaimBundleInput.SupportingInfoEntry.builder()
                                .sequence(2).categoryCode("last-menstrual-period")
                                .timingDate(LocalDate.of(2025, 1, 1)).build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getSupportingInfo()).hasSize(2);
        assertThat(claim.getSupportingInfo().get(0).getCategory().getCodingFirstRep().getCode())
                .isEqualTo("temperature");
        assertThat(claim.getSupportingInfo().get(1).getCategory().getCodingFirstRep().getCode())
                .isEqualTo("last-menstrual-period");
    }

    // ── Claim episode extension ───────────────────────────────────────────────

    @Test
    void build_claim_episodeExtensionIsSet() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .episodeSystem("http://provider.com/episode").episodeValue("episode-1")
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        boolean hasEpisode = claim.getExtension().stream()
                .anyMatch(e -> NphiesProfiles.EXT_EPISODE.equals(e.getUrl())
                        && e.getValue() instanceof Identifier id
                        && "episode-1".equals(id.getValue()));
        assertThat(hasEpisode).isTrue();
    }

    @Test
    void build_claim_noEpisodeExtensionWhenNotProvided() {
        Bundle bundle = parse(minimalInput());
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        boolean hasEpisode = claim.getExtension().stream()
                .anyMatch(e -> NphiesProfiles.EXT_EPISODE.equals(e.getUrl()));
        assertThat(hasEpisode).isFalse();
    }

    // ── Item payer-share extension ────────────────────────────────────────────

    @Test
    void build_item_payerShareExtensionIsSet() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .items(List.of(ClaimBundleInput.ClaimItemEntry.builder()
                        .sequence(1).productCode("96037-00-00")
                        .servicedDate(LocalDate.of(2025, 1, 15))
                        .unitPrice(BigDecimal.valueOf(1000)).net(BigDecimal.valueOf(1000))
                        .payerShareAmount(BigDecimal.valueOf(1000))
                        .build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        boolean hasPayerShare = claim.getItemFirstRep().getExtension().stream()
                .anyMatch(e -> NphiesProfiles.EXT_PAYER_SHARE.equals(e.getUrl()));
        assertThat(hasPayerShare).isTrue();
    }

    // ── Item patientInvoice extension ─────────────────────────────────────────

    @Test
    void build_item_patientInvoiceExtensionIsSet() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .items(List.of(ClaimBundleInput.ClaimItemEntry.builder()
                        .sequence(1).productCode("96037-00-00")
                        .servicedDate(LocalDate.of(2025, 1, 15))
                        .unitPrice(BigDecimal.valueOf(100)).net(BigDecimal.valueOf(100))
                        .patientInvoiceSystem("http://provider.com/invoice").patientInvoiceValue("INV-001")
                        .build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        boolean hasInvoice = claim.getItemFirstRep().getExtension().stream()
                .anyMatch(e -> NphiesProfiles.EXT_PATIENT_INVOICE.equals(e.getUrl())
                        && e.getValue() instanceof Identifier id
                        && "INV-001".equals(id.getValue()));
        assertThat(hasInvoice).isTrue();
    }

    // ── Item servicedPeriod ───────────────────────────────────────────────────

    @Test
    void build_item_servicedPeriodIsUsedWhenSet() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .items(List.of(ClaimBundleInput.ClaimItemEntry.builder()
                        .sequence(1).productCode("MED-001")
                        .servicedPeriodStart(LocalDate.of(2025, 1, 10))
                        .servicedPeriodEnd(LocalDate.of(2025, 1, 20))
                        .unitPrice(BigDecimal.valueOf(65)).net(BigDecimal.valueOf(195))
                        .build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getItemFirstRep().getServiced()).isInstanceOf(Period.class);
        Period p = (Period) claim.getItemFirstRep().getServiced();
        assertThat(p.getStart()).isNotNull();
        assertThat(p.getEnd()).isNotNull();
    }

    @Test
    void build_item_servicedDateIsUsedWhenNoPeriod() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .items(List.of(ClaimBundleInput.ClaimItemEntry.builder()
                        .sequence(1).productCode("PROC-001")
                        .servicedDate(LocalDate.of(2025, 1, 15))
                        .unitPrice(BigDecimal.valueOf(100)).net(BigDecimal.valueOf(100))
                        .build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getItemFirstRep().getServiced()).isInstanceOf(DateType.class);
    }

    // ── PractitionerRole active configurable ─────────────────────────────────

    @Test
    void build_practitionerRole_activeIsFalseWhenConfigured() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .careTeam(List.of(ClaimBundleInput.CareTeamMember.builder()
                        .sequence(1).practitionerLicense("PRAC-001")
                        .firstName("Dr").familyName("Smith").roleCode("doctor")
                        .active(false).build()))
                .build();
        Bundle bundle = parse(input);
        PractitionerRole role = (PractitionerRole) bundle.getEntry().get(6).getResource();
        assertThat(role.getActive()).isFalse();
    }

    // ── PractitionerRole practitioner identifier system ───────────────────────

    @Test
    void build_practitionerRole_practitionerIdentifierUsesLicenseSystem() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .careTeam(List.of(ClaimBundleInput.CareTeamMember.builder()
                        .sequence(1).practitionerLicense("2600097662")
                        .firstName("Dr").familyName("Smith").roleCode("doctor").build()))
                .build();
        Bundle bundle = parse(input);
        PractitionerRole role = (PractitionerRole) bundle.getEntry().get(6).getResource();
        assertThat(role.getPractitioner().getIdentifier().getSystem())
                .isEqualTo(NphiesProfiles.SYSTEM_PRACTITIONER_LICENSE);
    }

    // ── Supporting info attachment ────────────────────────────────────────────

    @Test
    void build_supportingInfo_attachmentIsAddedWhenProvided() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .supportingInfo(List.of(ClaimBundleInput.SupportingInfoEntry.builder()
                        .sequence(1).categoryCode("attachment")
                        .attachmentContentType("application/pdf")
                        .attachmentTitle("Discharge Summary")
                        .attachmentData("AAAA")
                        .attachmentCreation(LocalDate.of(2025, 1, 1))
                        .build()))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        Claim.SupportingInformationComponent si = claim.getSupportingInfoFirstRep();
        assertThat(si.getValue()).isInstanceOf(Attachment.class);
        Attachment att = (Attachment) si.getValue();
        assertThat(att.getContentType()).isEqualTo("application/pdf");
        assertThat(att.getTitle()).isEqualTo("Discharge Summary");
    }

    // ── Claim total ───────────────────────────────────────────────────────────

    @Test
    void build_claim_totalIsSetWhenProvided() {
        ClaimBundleInput input = ClaimBundleInput.builder()
                .requestId("b1").providerBaseUrl(BASE_URL)
                .useType("claim").claimType("institutional").priority("normal")
                .patientNationalId("1234567890").patientFirstName("X").patientFamilyName("Y")
                .patientDob(LocalDate.of(1990, 1, 1)).patientGender("male")
                .memberId("M1").payerLicenseNo(PAYER_LICENSE).payerName("Ins")
                .providerLicenseNo(PROVIDER_LICENSE).providerName("Hosp")
                .billablePeriodStart(LocalDate.of(2025, 1, 1)).billablePeriodEnd(LocalDate.of(2025, 1, 31))
                .totalAmount(BigDecimal.valueOf(3945))
                .build();
        Bundle bundle = parse(input);
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.getTotal()).isNotNull();
        assertThat(claim.getTotal().getValue()).isEqualByComparingTo(BigDecimal.valueOf(3945));
        assertThat(claim.getTotal().getCurrency()).isEqualTo("SAR");
    }

    @Test
    void build_claim_totalIsAbsentWhenNotProvided() {
        Bundle bundle = parse(minimalInput());
        Claim claim = (Claim) bundle.getEntry().get(1).getResource();
        assertThat(claim.hasTotal()).isFalse();
    }
}
