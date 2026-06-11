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
 * Bundle entry order (per NPHIES IG):
 * <ol>
 *   <li>MessageHeader</li>
 *   <li>CoverageEligibilityRequest</li>
 *   <li>Patient</li>
 *   <li>Coverage</li>
 *   <li>Organization — provider</li>
 *   <li>Organization — insurer</li>
 * </ol>
 *
 * All inter-resource references use {@code urn:uuid:{id}} fullUrls so the
 * bundle is self-contained and portable across environments.
 *
 * Usage:
 * <pre>
 *   String json = bundleBuilder.build(input);
 *   gatewayClient.submitBundle(tenantId, apiBaseUrl, json);
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoverageEligibilityRequestBundleBuilder {

    private final IParser fhirJsonParser;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds and serialises a NPHIES CoverageEligibilityRequest message Bundle.
     *
     * @param input validated caller-supplied request data
     * @return FHIR Bundle as a compact JSON string, ready to POST to /r4/Bundle
     */
    public String build(EligibilityRequestInput input) {
        validate(input);

        // Mint stable UUIDs for all resources (deterministic within a request)
        String msgHeaderId    = UUID.randomUUID().toString();
        String cerqId         = UUID.randomUUID().toString();
        String patientId      = UUID.randomUUID().toString();
        String coverageId     = UUID.randomUUID().toString();
        String providerId     = UUID.randomUUID().toString();
        String insurerId      = UUID.randomUUID().toString();

        Organization provider = buildProviderOrg(providerId, input);
        Organization insurer  = buildInsurerOrg(insurerId, input);
        Patient      patient  = buildPatient(patientId, input);
        Coverage     coverage = buildCoverage(coverageId, patientId, insurerId, input);

        CoverageEligibilityRequest cerq = buildCoverageEligibilityRequest(
                cerqId, patientId, coverageId, providerId, insurerId, input);

        MessageHeader msgHeader = buildMessageHeader(
                msgHeaderId, cerqId, input.getProviderLicenseNumber(),
                input.getPayerLicenseNumber());

        Bundle bundle = assembleBundle(input.getRequestId(), List.of(
                entry(msgHeaderId,  msgHeader),
                entry(cerqId,       cerq),
                entry(patientId,    patient),
                entry(coverageId,   coverage),
                entry(providerId,   provider),
                entry(insurerId,    insurer)
        ));

        log.debug("Built CoverageEligibilityRequest bundle [bundleId={}, requestId={}]",
                bundle.getId(), input.getRequestId());

        return fhirJsonParser.encodeResourceToString(bundle);
    }

    // ── Resource builders ─────────────────────────────────────────────────────

    private Bundle assembleBundle(String requestId, List<Bundle.BundleEntryComponent> entries) {
        Bundle bundle = new Bundle();
        bundle.setId(requestId);
        bundle.getMeta().addProfile(NphiesProfiles.BUNDLE);
        bundle.setType(Bundle.BundleType.MESSAGE);
        bundle.setTimestamp(new Date());
        entries.forEach(bundle::addEntry);
        return bundle;
    }

    private MessageHeader buildMessageHeader(String id, String cerqId,
                                              String providerLicense, String payerLicense) {
        MessageHeader hdr = new MessageHeader();
        hdr.setId(id);
        hdr.getMeta().addProfile(NphiesProfiles.MESSAGE_HEADER);

        // Event: eligibility-request
        hdr.setEvent(new Coding()
                .setSystem(NphiesProfiles.CS_MESSAGE_EVENTS)
                .setCode(NphiesEndpoints.EVENT_ELIGIBILITY_REQUEST));

        // Source endpoint — the sending provider system
        hdr.getSource().setEndpoint("http://" + providerLicense + ".nphies.sa");

        // Destination — the insurer/payer
        hdr.addDestination()
                .setEndpoint("http://" + payerLicense + ".nphies.sa");

        // Focus: the CoverageEligibilityRequest
        hdr.addFocus(new Reference("urn:uuid:" + cerqId));

        return hdr;
    }

    private CoverageEligibilityRequest buildCoverageEligibilityRequest(
            String id, String patientId, String coverageId,
            String providerId, String insurerId, EligibilityRequestInput input) {

        CoverageEligibilityRequest cerq = new CoverageEligibilityRequest();
        cerq.setId(id);
        cerq.getMeta().addProfile(NphiesProfiles.ELIGIBILITY_REQUEST);

        cerq.setStatus(CoverageEligibilityRequest.EligibilityRequestStatus.ACTIVE);

        // Purposes
        List<String> purposes = (input.getPurposes() == null || input.getPurposes().isEmpty())
                ? List.of("benefits")
                : input.getPurposes();
        purposes.forEach(p -> cerq.addPurpose(
                CoverageEligibilityRequest.EligibilityRequestPurpose.fromCode(p)));

        cerq.setPatient(new Reference("urn:uuid:" + patientId));
        cerq.setCreated(new Date());

        // Serviced date
        LocalDate svcDate = input.getServicedDate() != null ? input.getServicedDate() : LocalDate.now();
        cerq.setServiced(new DateType(
                Date.from(svcDate.atStartOfDay().toInstant(ZoneOffset.UTC))));

        cerq.setInsurer(new Reference("urn:uuid:" + insurerId));
        cerq.setProvider(new Reference("urn:uuid:" + providerId));

        // Insurance component
        CoverageEligibilityRequest.InsuranceComponent insuranceComponent =
                new CoverageEligibilityRequest.InsuranceComponent();
        insuranceComponent.setCoverage(new Reference("urn:uuid:" + coverageId));
        cerq.addInsurance(insuranceComponent);

        return cerq;
    }

    private Patient buildPatient(String id, EligibilityRequestInput input) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.getMeta().addProfile(NphiesProfiles.PATIENT);

        // Identifier: Saudi national ID (starts with 1) vs Iqama (starts with 2)
        String nationalId = input.getPatientNationalId().trim();
        String idSystem = nationalId.startsWith("1")
                ? NphiesProfiles.SYSTEM_NATIONAL_ID
                : NphiesProfiles.SYSTEM_IQAMA;

        patient.addIdentifier()
                .setSystem(idSystem)
                .setValue(nationalId);

        patient.addName()
                .setFamily(input.getPatientFamilyName())
                .addGiven(input.getPatientFirstName());

        patient.setGender(Enumerations.AdministrativeGender.fromCode(input.getPatientGender()));

        patient.setBirthDate(Date.from(
                input.getPatientDateOfBirth().atStartOfDay().toInstant(ZoneOffset.UTC)));

        return patient;
    }

    private Coverage buildCoverage(String id, String patientId, String insurerId,
                                    EligibilityRequestInput input) {
        Coverage coverage = new Coverage();
        coverage.setId(id);
        coverage.getMeta().addProfile(NphiesProfiles.COVERAGE);

        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);

        coverage.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_MEMBER_ID)
                .setValue(input.getMemberId());

        // Subscriber relationship
        coverage.setRelationship(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_RELATIONSHIP)
                .setCode(input.getCoverageRelationship())));

        coverage.setSubscriber(new Reference("urn:uuid:" + patientId));
        coverage.setSubscriberId(input.getMemberId());
        coverage.setBeneficiary(new Reference("urn:uuid:" + patientId));

        coverage.addPayor(new Reference("urn:uuid:" + insurerId));

        return coverage;
    }

    private Organization buildProviderOrg(String id, EligibilityRequestInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.PROVIDER_ORGANIZATION);
        org.setActive(true);
        org.setName(input.getProviderName());
        org.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_PROVIDER_LICENSE)
                .setValue(input.getProviderLicenseNumber());
        return org;
    }

    private Organization buildInsurerOrg(String id, EligibilityRequestInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.INSURER_ORGANIZATION);
        org.setActive(true);
        org.setName(input.getPayerName());
        org.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_PAYER_LICENSE)
                .setValue(input.getPayerLicenseNumber());
        return org;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Wraps a resource in a Bundle entry with a {@code urn:uuid:} fullUrl. */
    private Bundle.BundleEntryComponent entry(String id, Resource resource) {
        return new Bundle.BundleEntryComponent()
                .setFullUrl("urn:uuid:" + id)
                .setResource(resource);
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
