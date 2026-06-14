package com.amins.nphies.fhir.bundle;

import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.fhir.NphiesProfiles;
import com.amins.nphies.gateway.NphiesEndpoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Builds a NPHIES FHIR message Bundle containing a CoverageEligibilityRequest.
 *
 * Bundle entry order (per NPHIES IG sample format):
 * <ol>
 *   <li>MessageHeader          — urn:uuid fullUrl</li>
 *   <li>CoverageEligibilityRequest</li>
 *   <li>Coverage</li>
 *   <li>Organization — provider</li>
 *   <li>Patient</li>
 *   <li>Organization — insurer</li>
 * </ol>
 *
 * All entries except MessageHeader use {@code {providerBaseUrl}/{ResourceType}/{id}} fullUrls
 * so that inter-resource references resolve within the bundle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoverageEligibilityRequestBundleBuilder {

    private final IParser fhirJsonParser;

    // ── Public API ────────────────────────────────────────────────────────────

    public String build(EligibilityRequestInput input) {
        validate(input);

        String baseUrl = input.getProviderBaseUrl();

        // Stable UUIDs for all resources
        String msgHeaderId = UUID.randomUUID().toString();
        String cerqId      = UUID.randomUUID().toString();
        String patientId   = UUID.randomUUID().toString();
        String coverageId  = UUID.randomUUID().toString();
        String providerId  = UUID.randomUUID().toString();
        String insurerId   = UUID.randomUUID().toString();

        // Canonical http-based URLs (fullUrl and internal references)
        String cerqUrl        = url(baseUrl, "CoverageEligibilityRequest", cerqId);
        String patientUrl     = url(baseUrl, "Patient",      patientId);
        String coverageUrl    = url(baseUrl, "Coverage",     coverageId);
        String providerOrgUrl = url(baseUrl, "Organization", providerId);
        String insurerOrgUrl  = url(baseUrl, "Organization", insurerId);

        Organization provider = buildProviderOrg(providerId, input);
        Organization insurer  = buildInsurerOrg(insurerId,  input);
        Patient      patient  = buildPatient(patientId, providerOrgUrl, input);
        Coverage     coverage = buildCoverage(coverageId, patientUrl, insurerOrgUrl, providerOrgUrl, input);

        CoverageEligibilityRequest cerq = buildCoverageEligibilityRequest(
                cerqId, cerqUrl, patientUrl, coverageUrl, providerOrgUrl, insurerOrgUrl, input);

        MessageHeader msgHeader = buildMessageHeader(
                msgHeaderId, cerqUrl, baseUrl,
                input.getProviderLicenseNumber(), input.getPayerLicenseNumber());

        Bundle bundle = assembleBundle(input.getRequestId(), List.of(
                entry("urn:uuid:" + msgHeaderId, msgHeader),
                entry(cerqUrl,        cerq),
                entry(coverageUrl,    coverage),
                entry(providerOrgUrl, provider),
                entry(patientUrl,     patient),
                entry(insurerOrgUrl,  insurer)
        ));

        log.debug("Built CoverageEligibilityRequest bundle [bundleId={}, requestId={}]",
                bundle.getId(), input.getRequestId());

        return fhirJsonParser.encodeResourceToString(bundle);
    }

    // ── Resource builders ─────────────────────────────────────────────────────

    private Bundle assembleBundle(String requestId, List<Bundle.BundleEntryComponent> entries) {
        Bundle bundle = new Bundle();
        bundle.setId(requestId);
        bundle.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.BUNDLE));
        bundle.setType(Bundle.BundleType.MESSAGE);
        bundle.setTimestamp(new Date());
        entries.forEach(bundle::addEntry);
        return bundle;
    }

    private MessageHeader buildMessageHeader(String id, String cerqUrl, String providerBaseUrl,
                                             String providerLicense, String payerLicense) {
        MessageHeader hdr = new MessageHeader();
        hdr.setId(id);
        hdr.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.MESSAGE_HEADER));

        hdr.setEvent(new Coding()
                .setSystem(NphiesProfiles.CS_MESSAGE_EVENTS)
                .setCode(NphiesEndpoints.EVENT_ELIGIBILITY_REQUEST));

        // Sender — provider organisation identified by license number
        Reference sender = new Reference();
        sender.setType("Organization");
        sender.setIdentifier(new Identifier()
                .setSystem(NphiesProfiles.SYSTEM_PROVIDER_LICENSE)
                .setValue(providerLicense));
        hdr.setSender(sender);

        // Source — provider system endpoint
        hdr.getSource().setEndpoint(providerBaseUrl);

        // Destination — fixed NPHIES routing endpoint + payer receiver
        Reference receiver = new Reference();
        receiver.setType("Organization");
        receiver.setIdentifier(new Identifier()
                .setSystem(NphiesProfiles.SYSTEM_PAYER_LICENSE)
                .setValue(payerLicense));
        hdr.addDestination()
                .setEndpoint(NphiesEndpoints.NPHIES_DESTINATION_ENDPOINT)
                .setReceiver(receiver);

        // Focus: the CoverageEligibilityRequest
        hdr.addFocus(new Reference(cerqUrl));

        return hdr;
    }

    private CoverageEligibilityRequest buildCoverageEligibilityRequest(
            String id, String cerqUrl, String patientUrl, String coverageUrl,
            String providerOrgUrl, String insurerOrgUrl, EligibilityRequestInput input) {

        CoverageEligibilityRequest cerq = new CoverageEligibilityRequest();
        cerq.setId(id);
        cerq.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.ELIGIBILITY_REQUEST));
        cerq.setStatus(CoverageEligibilityRequest.EligibilityRequestStatus.ACTIVE);

        // Identifier — system is the collection URL, value is the resource ID
        String cerqSystem = cerqUrl.substring(0, cerqUrl.lastIndexOf('/'));
        cerq.addIdentifier().setSystem(cerqSystem).setValue(id);

        // Purposes
        List<String> purposes = (input.getPurposes() == null || input.getPurposes().isEmpty())
                ? List.of("benefits") : input.getPurposes();
        purposes.forEach(p -> cerq.addPurpose(
                CoverageEligibilityRequest.EligibilityRequestPurpose.fromCode(p)));

        // Priority — stat for all NPHIES eligibility requests
        cerq.setPriority(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_PROCESS_PRIORITY)
                .setCode("stat")));

        cerq.setPatient(new Reference(patientUrl));
        cerq.setCreated(new Date());

        // Serviced period
        LocalDate svcStart = input.getServicedDate() != null ? input.getServicedDate() : LocalDate.now();
        LocalDate svcEnd   = input.getServicedPeriodEnd() != null ? input.getServicedPeriodEnd() : svcStart;
        cerq.setServiced(new Period().setStart(toDate(svcStart)).setEnd(toDate(svcEnd)));

        cerq.setInsurer(new Reference(insurerOrgUrl));
        cerq.setProvider(new Reference(providerOrgUrl));

        // Insurance component
        CoverageEligibilityRequest.InsuranceComponent ins =
                new CoverageEligibilityRequest.InsuranceComponent();
        ins.setCoverage(new Reference(coverageUrl));
        if (input.getBusinessArrangement() != null) {
            ins.setBusinessArrangement(input.getBusinessArrangement());
        }
        cerq.addInsurance(ins);

        return cerq;
    }

    private Patient buildPatient(String id, String providerOrgUrl, EligibilityRequestInput input) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.PATIENT));
        patient.setActive(true);

        // Identifier — national ID (starts with 1) vs Iqama (starts with 2)
        String nationalId = input.getPatientNationalId().trim();
        boolean isSaudi   = nationalId.startsWith("1");
        String idSystem   = isSaudi ? NphiesProfiles.SYSTEM_NATIONAL_ID : NphiesProfiles.SYSTEM_IQAMA;
        String idTypeCode = isSaudi ? "NI" : "PRC";

        Identifier ident = patient.addIdentifier();
        ident.getType().addCoding()
                .setSystem(NphiesProfiles.CS_V2_0203)
                .setCode(idTypeCode);

        // Country extension for Iqama holders
        if (!isSaudi && input.getPatientNationalityCode() != null) {
            ident.addExtension()
                    .setUrl(NphiesProfiles.EXT_IDENTIFIER_COUNTRY)
                    .setValue(new CodeableConcept().addCoding(new Coding()
                            .setSystem("urn:iso:std:iso:3166")
                            .setCode(input.getPatientNationalityCode())
                            .setDisplay(input.getPatientNationalityDisplay())));
        }
        ident.setSystem(idSystem).setValue(nationalId);

        patient.setManagingOrganization(new Reference(providerOrgUrl));

        // Name — official use, all given names, full text
        List<String> givenNames = (input.getPatientGivenNames() != null
                && !input.getPatientGivenNames().isEmpty())
                ? input.getPatientGivenNames()
                : List.of(input.getPatientFirstName());
        HumanName name = patient.addName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        name.setFamily(input.getPatientFamilyName());
        givenNames.forEach(name::addGiven);
        name.setText(String.join(" ", givenNames) + " " + input.getPatientFamilyName());

        // Gender + KSA administrative gender extension
        patient.setGender(Enumerations.AdministrativeGender.fromCode(input.getPatientGender()));
        patient.getGenderElement().addExtension()
                .setUrl(NphiesProfiles.EXT_KSA_ADMIN_GENDER)
                .setValue(new CodeableConcept().addCoding(new Coding()
                        .setSystem(NphiesProfiles.CS_KSA_ADMIN_GENDER)
                        .setCode(input.getPatientGender())));

        patient.setBirthDate(toDate(input.getPatientDateOfBirth()));

        if (input.getPatientPhone() != null) {
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(input.getPatientPhone());
        }

        if (input.getPatientMaritalStatus() != null) {
            patient.setMaritalStatus(new CodeableConcept().addCoding(new Coding()
                    .setSystem(NphiesProfiles.CS_V3_MARITAL_STATUS)
                    .setCode(input.getPatientMaritalStatus())));
        }

        return patient;
    }

    private Coverage buildCoverage(String id, String patientUrl, String insurerOrgUrl,
                                   String providerOrgUrl, EligibilityRequestInput input) {
        Coverage coverage = new Coverage();
        coverage.setId(id);
        coverage.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.COVERAGE));
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);

        // Member ID
        String memIdSystem = input.getMemberIdSystem() != null
                ? input.getMemberIdSystem() : NphiesProfiles.SYSTEM_MEMBER_ID;
        coverage.addIdentifier().setSystem(memIdSystem).setValue(input.getMemberId());

        // Coverage period
        if (input.getCoveragePeriodStart() != null || input.getCoveragePeriodEnd() != null) {
            Period period = new Period();
            if (input.getCoveragePeriodStart() != null) period.setStart(toDate(input.getCoveragePeriodStart()));
            if (input.getCoveragePeriodEnd() != null)   period.setEnd(toDate(input.getCoveragePeriodEnd()));
            coverage.setPeriod(period);
        }

        // Coverage type
        coverage.setType(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_COVERAGE_TYPE)
                .setCode(input.getCoverageType())
                .setDisplay(input.getCoverageTypeDisplay())));

        // Subscriber relationship
        coverage.setRelationship(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_RELATIONSHIP)
                .setCode(input.getCoverageRelationship())));

        coverage.setSubscriber(new Reference(patientUrl));
        coverage.setSubscriberId(input.getMemberId());
        coverage.setBeneficiary(new Reference(patientUrl));
        coverage.setPolicyHolder(new Reference(providerOrgUrl));
        coverage.addPayor(new Reference(insurerOrgUrl));

        return coverage;
    }

    private Organization buildProviderOrg(String id, EligibilityRequestInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.PROVIDER_ORGANIZATION));
        org.setActive(true);
        org.setName(input.getProviderName());
        org.addIdentifier()
                .setUse(Identifier.IdentifierUse.OFFICIAL)
                .setSystem(NphiesProfiles.SYSTEM_PROVIDER_LICENSE)
                .setValue(input.getProviderLicenseNumber());
        org.addType().addCoding()
                .setSystem(NphiesProfiles.CS_ORG_TYPE)
                .setCode("prov");
        return org;
    }

    private Organization buildInsurerOrg(String id, EligibilityRequestInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.INSURER_ORGANIZATION));
        org.setActive(true);
        org.setName(input.getPayerName());
        org.addIdentifier()
                .setUse(Identifier.IdentifierUse.OFFICIAL)
                .setSystem(NphiesProfiles.SYSTEM_PAYER_LICENSE)
                .setValue(input.getPayerLicenseNumber());
        org.addType().addCoding()
                .setSystem(NphiesProfiles.CS_ORG_TYPE)
                .setCode("ins");
        return org;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bundle.BundleEntryComponent entry(String fullUrl, Resource resource) {
        return new Bundle.BundleEntryComponent().setFullUrl(fullUrl).setResource(resource);
    }

    private String url(String base, String resourceType, String id) {
        return base + "/" + resourceType + "/" + id;
    }

    private Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private void validate(EligibilityRequestInput input) {
        if (input.getPurposes() != null) {
            List<String> allowed = List.of("benefits", "discovery", "validation", "auth-requirements");
            input.getPurposes().forEach(p -> {
                if (!allowed.contains(p)) {
                    throw new IllegalArgumentException(
                            "Invalid eligibility purpose '" + p + "'. Allowed: " + allowed);
                }
            });
        }
        String gender = input.getPatientGender();
        if (!List.of("male", "female", "other", "unknown").contains(gender)) {
            throw new IllegalArgumentException(
                    "Invalid patient gender '" + gender + "'. Must be male|female|other|unknown");
        }
    }
}
