package com.amins.nphies.fhir.bundle;

import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.fhir.NphiesProfiles;
import com.amins.nphies.gateway.NphiesEndpoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Builds a NPHIES FHIR message Bundle containing a Claim resource.
 *
 * Bundle entry order (per NPHIES IG):
 * 1. MessageHeader
 * 2. Claim
 * 3. Patient
 * 4. Coverage
 * 5. Organization (provider)
 * 6. Organization (insurer)
 * 7. Practitioner (per care team member)
 * 8. Encounter
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimBundleBuilder {

    private final IParser fhirJsonParser;

    public String build(ClaimBundleInput input) {
        String msgHeaderId = UUID.randomUUID().toString();
        String claimResId  = UUID.randomUUID().toString();
        String patientId   = UUID.randomUUID().toString();
        String coverageId  = UUID.randomUUID().toString();
        String providerId  = UUID.randomUUID().toString();
        String insurerId   = UUID.randomUUID().toString();
        String encounterId = UUID.randomUUID().toString();

        // Build care team practitioners
        List<String> practitionerIds = new ArrayList<>();
        List<Practitioner> practitioners = new ArrayList<>();
        if (input.getCareTeam() != null) {
            for (ClaimBundleInput.CareTeamMember member : input.getCareTeam()) {
                String practId = UUID.randomUUID().toString();
                practitionerIds.add(practId);
                practitioners.add(buildPractitioner(practId, member));
            }
        }

        Organization provider = buildProviderOrg(providerId, input);
        Organization insurer  = buildInsurerOrg(insurerId, input);
        Patient patient       = buildPatient(patientId, input);
        Coverage coverage     = buildCoverage(coverageId, patientId, insurerId, input);
        Encounter encounter   = buildEncounter(encounterId, patientId, providerId, input);
        Claim claim           = buildClaim(claimResId, patientId, coverageId, providerId,
                insurerId, encounterId, practitionerIds, input);

        MessageHeader msgHeader = buildMessageHeader(msgHeaderId, claimResId,
                input.getProviderLicenseNo(), input.getPayerLicenseNo(), input.getUseType());

        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(entry(msgHeaderId, msgHeader));
        entries.add(entry(claimResId,  claim));
        entries.add(entry(patientId,   patient));
        entries.add(entry(coverageId,  coverage));
        entries.add(entry(providerId,  provider));
        entries.add(entry(insurerId,   insurer));
        for (int i = 0; i < practitioners.size(); i++) {
            entries.add(entry(practitionerIds.get(i), practitioners.get(i)));
        }
        entries.add(entry(encounterId, encounter));

        Bundle bundle = assembleBundle(input.getRequestId(), entries);

        log.debug("Built Claim bundle [bundleId={}, claimId={}]", bundle.getId(), input.getRequestId());
        return fhirJsonParser.encodeResourceToString(bundle);
    }

    private Bundle assembleBundle(String requestId, List<Bundle.BundleEntryComponent> entries) {
        Bundle bundle = new Bundle();
        bundle.setId(requestId);
        bundle.getMeta().addProfile(NphiesProfiles.BUNDLE);
        bundle.setType(Bundle.BundleType.MESSAGE);
        bundle.setTimestamp(new Date());
        entries.forEach(bundle::addEntry);
        return bundle;
    }

    private MessageHeader buildMessageHeader(String id, String claimResId,
                                              String providerLicense, String payerLicense,
                                              String useType) {
        MessageHeader hdr = new MessageHeader();
        hdr.setId(id);
        hdr.getMeta().addProfile(NphiesProfiles.MESSAGE_HEADER);

        String eventCode = "preauthorization".equals(useType) ? "priorauth-request" : "claim-request";
        hdr.setEvent(new Coding()
                .setSystem(NphiesProfiles.CS_MESSAGE_EVENTS)
                .setCode(eventCode));

        hdr.getSource().setEndpoint("http://" + providerLicense + ".nphies.sa");
        hdr.addDestination().setEndpoint("http://" + payerLicense + ".nphies.sa");
        hdr.addFocus(new Reference("urn:uuid:" + claimResId));
        return hdr;
    }

    private Claim buildClaim(String id, String patientId, String coverageId,
                              String providerId, String insurerId, String encounterId,
                              List<String> practitionerIds, ClaimBundleInput input) {
        Claim claim = new Claim();
        claim.setId(id);
        claim.getMeta().addProfile(NphiesProfiles.CLAIM);

        claim.setStatus(Claim.ClaimStatus.ACTIVE);

        // Use
        claim.setUse(Claim.Use.fromCode(input.getUseType()));

        // Type
        claim.setType(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_CLAIM_TYPE)
                .setCode(input.getClaimType())));

        // Priority
        String priorityCode = input.getPriority() != null ? input.getPriority() : "normal";
        claim.setPriority(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_PROCESS_PRIORITY)
                .setCode(priorityCode)));

        claim.setPatient(new Reference("urn:uuid:" + patientId));
        claim.setInsurer(new Reference("urn:uuid:" + insurerId));
        claim.setProvider(new Reference("urn:uuid:" + providerId));

        // Billable period
        Period billablePeriod = new Period();
        billablePeriod.setStart(Date.from(input.getBillablePeriodStart().atStartOfDay().toInstant(ZoneOffset.UTC)));
        billablePeriod.setEnd(Date.from(input.getBillablePeriodEnd().atStartOfDay().toInstant(ZoneOffset.UTC)));
        claim.setBillablePeriod(billablePeriod);

        claim.setCreated(new Date());

        // Insurance
        Claim.InsuranceComponent insurance = new Claim.InsuranceComponent();
        insurance.setSequence(1);
        insurance.setFocal(true);
        insurance.setCoverage(new Reference("urn:uuid:" + coverageId));
        claim.addInsurance(insurance);

        // Care team
        if (input.getCareTeam() != null) {
            for (int i = 0; i < input.getCareTeam().size(); i++) {
                ClaimBundleInput.CareTeamMember member = input.getCareTeam().get(i);
                Claim.CareTeamComponent ct = new Claim.CareTeamComponent();
                ct.setSequence(member.getSequence());
                ct.setProvider(new Reference("urn:uuid:" + practitionerIds.get(i)));
                ct.setRole(new CodeableConcept().addCoding(new Coding()
                        .setSystem(NphiesProfiles.CS_CARE_TEAM_ROLE)
                        .setCode(member.getRoleCode() != null ? member.getRoleCode() : "primary")));
                claim.addCareTeam(ct);
            }
        }

        // Diagnoses
        if (input.getDiagnoses() != null) {
            for (ClaimBundleInput.DiagnosisEntry diag : input.getDiagnoses()) {
                Claim.DiagnosisComponent dc = new Claim.DiagnosisComponent();
                dc.setSequence(diag.getSequence());
                dc.setDiagnosis(new CodeableConcept().addCoding(new Coding()
                        .setSystem(NphiesProfiles.CS_ICD10)
                        .setCode(diag.getIcd10Code())
                        .setDisplay(diag.getIcd10Display())));
                if (diag.getDiagnosisType() != null) {
                    dc.addType(new CodeableConcept().addCoding(new Coding()
                            .setSystem(NphiesProfiles.CS_DIAGNOSIS_TYPE)
                            .setCode(diag.getDiagnosisType())));
                }
                claim.addDiagnosis(dc);
            }
        }

        // Items
        if (input.getItems() != null) {
            for (ClaimBundleInput.ClaimItemEntry itemEntry : input.getItems()) {
                Claim.ItemComponent item = new Claim.ItemComponent();
                item.setSequence(itemEntry.getSequence());

                // Care team sequence references
                if (itemEntry.getCareTeamSeqs() != null) {
                    for (int seq : itemEntry.getCareTeamSeqs()) {
                        item.addCareTeamSequence(seq);
                    }
                }

                // Diagnosis sequence references
                if (itemEntry.getDiagnosisSeqs() != null) {
                    for (int seq : itemEntry.getDiagnosisSeqs()) {
                        item.addDiagnosisSequence(seq);
                    }
                }

                String productSystem = itemEntry.getProductSystem() != null
                        ? itemEntry.getProductSystem() : NphiesProfiles.CS_PROCEDURE;
                item.setProductOrService(new CodeableConcept().addCoding(new Coding()
                        .setSystem(productSystem)
                        .setCode(itemEntry.getProductCode())));

                item.setServiced(new DateType(
                        Date.from(itemEntry.getServicedDate().atStartOfDay().toInstant(ZoneOffset.UTC))));

                BigDecimal qty = itemEntry.getQty() != null ? itemEntry.getQty() : BigDecimal.ONE;
                item.setQuantity(new Quantity().setValue(qty));

                Money unitPrice = new Money();
                unitPrice.setValue(itemEntry.getUnitPrice());
                unitPrice.setCurrency(input.getCurrency() != null ? input.getCurrency() : "SAR");
                item.setUnitPrice(unitPrice);

                Money net = new Money();
                net.setValue(itemEntry.getNet());
                net.setCurrency(input.getCurrency() != null ? input.getCurrency() : "SAR");
                item.setNet(net);

                claim.addItem(item);
            }
        }

        return claim;
    }

    private Patient buildPatient(String id, ClaimBundleInput input) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.getMeta().addProfile(NphiesProfiles.PATIENT);

        String nationalId = input.getPatientNationalId().trim();
        String idSystem = nationalId.startsWith("1")
                ? NphiesProfiles.SYSTEM_NATIONAL_ID
                : NphiesProfiles.SYSTEM_IQAMA;

        patient.addIdentifier().setSystem(idSystem).setValue(nationalId);
        patient.addName().setFamily(input.getPatientFamilyName()).addGiven(input.getPatientFirstName());
        patient.setGender(Enumerations.AdministrativeGender.fromCode(input.getPatientGender()));
        patient.setBirthDate(Date.from(input.getPatientDob().atStartOfDay().toInstant(ZoneOffset.UTC)));
        return patient;
    }

    private Coverage buildCoverage(String id, String patientId, String insurerId, ClaimBundleInput input) {
        Coverage coverage = new Coverage();
        coverage.setId(id);
        coverage.getMeta().addProfile(NphiesProfiles.COVERAGE);

        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        coverage.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_MEMBER_ID)
                .setValue(input.getMemberId());

        String rel = input.getCoverageRelationship() != null ? input.getCoverageRelationship() : "self";
        coverage.setRelationship(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_RELATIONSHIP)
                .setCode(rel)));

        coverage.setSubscriber(new Reference("urn:uuid:" + patientId));
        coverage.setSubscriberId(input.getMemberId());
        coverage.setBeneficiary(new Reference("urn:uuid:" + patientId));
        coverage.addPayor(new Reference("urn:uuid:" + insurerId));
        return coverage;
    }

    private Organization buildProviderOrg(String id, ClaimBundleInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.PROVIDER_ORGANIZATION);
        org.setActive(true);
        org.setName(input.getProviderName());
        org.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_PROVIDER_LICENSE)
                .setValue(input.getProviderLicenseNo());
        return org;
    }

    private Organization buildInsurerOrg(String id, ClaimBundleInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.INSURER_ORGANIZATION);
        org.setActive(true);
        org.setName(input.getPayerName());
        org.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_PAYER_LICENSE)
                .setValue(input.getPayerLicenseNo());
        return org;
    }

    private Practitioner buildPractitioner(String id, ClaimBundleInput.CareTeamMember member) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(id);
        practitioner.getMeta().addProfile(NphiesProfiles.PRACTITIONER);
        practitioner.setActive(true);
        practitioner.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_PRACTITIONER_LICENSE)
                .setValue(member.getPractitionerLicense());
        practitioner.addName()
                .setFamily(member.getFamilyName())
                .addGiven(member.getFirstName());
        return practitioner;
    }

    private Encounter buildEncounter(String id, String patientId, String providerId, ClaimBundleInput input) {
        Encounter encounter = new Encounter();
        encounter.setId(id);
        encounter.getMeta().addProfile(NphiesProfiles.ENCOUNTER);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        encounter.setClass_(new Coding()
                .setSystem(NphiesProfiles.CS_ENCOUNTER_CLASS)
                .setCode("AMB"));

        encounter.setSubject(new Reference("urn:uuid:" + patientId));
        encounter.setServiceProvider(new Reference("urn:uuid:" + providerId));

        Period period = new Period();
        period.setStart(Date.from(input.getBillablePeriodStart().atStartOfDay().toInstant(ZoneOffset.UTC)));
        period.setEnd(Date.from(input.getBillablePeriodEnd().atStartOfDay().toInstant(ZoneOffset.UTC)));
        encounter.setPeriod(period);

        return encounter;
    }

    private Bundle.BundleEntryComponent entry(String id, Resource resource) {
        return new Bundle.BundleEntryComponent()
                .setFullUrl("urn:uuid:" + id)
                .setResource(resource);
    }
}
