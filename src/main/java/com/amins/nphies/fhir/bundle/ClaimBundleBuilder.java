package com.amins.nphies.fhir.bundle;

import ca.uhn.fhir.parser.IParser;
import com.amins.nphies.fhir.NphiesProfiles;
import com.amins.nphies.gateway.NphiesEndpoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Builds a NPHIES FHIR message Bundle containing a Claim (or PriorAuth) resource.
 *
 * Bundle entry order (per NPHIES IG sample format):
 * 1. MessageHeader          — urn:uuid fullUrl
 * 2. Claim                  — {providerBaseUrl}/Claim/{id}
 * 3. Organization (provider)
 * 4. Patient
 * 5. Organization (insurer)
 * 6. Coverage
 * 7. PractitionerRole(s)    — one per care team member
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimBundleBuilder {

    private final IParser fhirJsonParser;

    public String build(ClaimBundleInput input) {
        String baseUrl = input.getProviderBaseUrl();

        String msgHeaderId = UUID.randomUUID().toString();
        String claimResId  = UUID.randomUUID().toString();
        String patientId   = UUID.randomUUID().toString();
        String coverageId  = UUID.randomUUID().toString();
        String providerId  = UUID.randomUUID().toString();
        String insurerId   = UUID.randomUUID().toString();

        String claimUrl       = url(baseUrl, "Claim",        claimResId);
        String patientUrl     = url(baseUrl, "Patient",      patientId);
        String coverageUrl    = url(baseUrl, "Coverage",     coverageId);
        String providerOrgUrl = url(baseUrl, "Organization", providerId);
        String insurerOrgUrl  = url(baseUrl, "Organization", insurerId);

        List<String> practRoleIds  = new ArrayList<>();
        List<String> practRoleUrls = new ArrayList<>();
        List<PractitionerRole> practRoles = new ArrayList<>();
        if (input.getCareTeam() != null) {
            for (ClaimBundleInput.CareTeamMember member : input.getCareTeam()) {
                String prId  = UUID.randomUUID().toString();
                String prUrl = url(baseUrl, "PractitionerRole", prId);
                practRoleIds.add(prId);
                practRoleUrls.add(prUrl);
                practRoles.add(buildPractitionerRole(prId, providerOrgUrl, member));
            }
        }

        Organization  provider = buildProviderOrg(providerId, input);
        Organization  insurer  = buildInsurerOrg(insurerId, input);
        Patient       patient  = buildPatient(patientId, providerOrgUrl, input);
        Coverage      coverage = buildCoverage(coverageId, patientUrl, insurerOrgUrl, input);
        Claim         claim    = buildClaim(claimResId, claimUrl, patientUrl, coverageUrl,
                providerOrgUrl, insurerOrgUrl, practRoleUrls, input);
        MessageHeader msgHeader = buildMessageHeader(msgHeaderId, claimUrl, baseUrl,
                input.getProviderLicenseNo(), input.getPayerLicenseNo(), input.getUseType());

        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(entry("urn:uuid:" + msgHeaderId, msgHeader));
        entries.add(entry(claimUrl,       claim));
        entries.add(entry(providerOrgUrl, provider));
        entries.add(entry(patientUrl,     patient));
        entries.add(entry(insurerOrgUrl,  insurer));
        entries.add(entry(coverageUrl,    coverage));
        for (int i = 0; i < practRoles.size(); i++) {
            entries.add(entry(practRoleUrls.get(i), practRoles.get(i)));
        }

        Bundle bundle = assembleBundle(input.getRequestId(), entries);

        log.debug("Built Claim bundle [bundleId={}, claimId={}]", bundle.getId(), input.getRequestId());
        return fhirJsonParser.encodeResourceToString(bundle);
    }

    private Bundle assembleBundle(String requestId, List<Bundle.BundleEntryComponent> entries) {
        Bundle bundle = new Bundle();
        bundle.setId(requestId);
        bundle.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.BUNDLE));
        bundle.setType(Bundle.BundleType.MESSAGE);
        bundle.setTimestamp(new Date());
        entries.forEach(bundle::addEntry);
        return bundle;
    }

    private MessageHeader buildMessageHeader(String id, String claimUrl, String providerBaseUrl,
                                              String providerLicense, String payerLicense,
                                              String useType) {
        MessageHeader hdr = new MessageHeader();
        hdr.setId(id);
        hdr.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.MESSAGE_HEADER));

        String eventCode = "preauthorization".equals(useType)
                ? NphiesEndpoints.EVENT_PRIORAUTH_REQUEST
                : NphiesEndpoints.EVENT_CLAIM_REQUEST;
        hdr.setEvent(new Coding()
                .setSystem(NphiesProfiles.CS_MESSAGE_EVENTS)
                .setCode(eventCode));

        Reference sender = new Reference();
        sender.setType("Organization");
        sender.setIdentifier(new Identifier()
                .setSystem(NphiesProfiles.SYSTEM_PROVIDER_LICENSE)
                .setValue(providerLicense));
        hdr.setSender(sender);

        hdr.getSource().setEndpoint(providerBaseUrl);

        Reference receiver = new Reference();
        receiver.setType("Organization");
        receiver.setIdentifier(new Identifier()
                .setSystem(NphiesProfiles.SYSTEM_PAYER_LICENSE)
                .setValue(payerLicense));
        hdr.addDestination()
                .setEndpoint(NphiesEndpoints.NPHIES_DESTINATION_ENDPOINT)
                .setReceiver(receiver);

        hdr.addFocus(new Reference(claimUrl));

        return hdr;
    }

    private Claim buildClaim(String id, String claimUrl, String patientUrl, String coverageUrl,
                              String providerOrgUrl, String insurerOrgUrl,
                              List<String> practRoleUrls, ClaimBundleInput input) {
        Claim claim = new Claim();
        claim.setId(id);
        claim.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.INSTITUTIONAL_CLAIM));

        String identSystem = input.getClaimIdentifierSystem() != null
                ? input.getClaimIdentifierSystem()
                : claimUrl.substring(0, claimUrl.lastIndexOf('/'));
        claim.addIdentifier().setSystem(identSystem).setValue(id);

        if (input.getEpisodeValue() != null) {
            claim.addExtension()
                    .setUrl(NphiesProfiles.EXT_EPISODE)
                    .setValue(new Identifier()
                            .setSystem(input.getEpisodeSystem())
                            .setValue(input.getEpisodeValue()));
        }

        claim.setStatus(Claim.ClaimStatus.ACTIVE);
        claim.setUse(Claim.Use.fromCode(input.getUseType()));

        claim.setType(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_CLAIM_TYPE)
                .setCode(input.getClaimType())));

        if (input.getClaimSubType() != null) {
            claim.setSubType(new CodeableConcept().addCoding(new Coding()
                    .setSystem(NphiesProfiles.CS_CLAIM_SUBTYPE)
                    .setCode(input.getClaimSubType())));
        }

        claim.setPatient(new Reference(patientUrl));
        claim.setCreated(new Date());
        claim.setInsurer(new Reference(insurerOrgUrl));
        claim.setProvider(new Reference(providerOrgUrl));

        claim.setPriority(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_PROCESS_PRIORITY)
                .setCode(input.getPriority())));

        if (input.getPayeeTypeCode() != null) {
            claim.setPayee(new Claim.PayeeComponent()
                    .setType(new CodeableConcept().addCoding(new Coding()
                            .setSystem(NphiesProfiles.CS_PAYEE_TYPE)
                            .setCode(input.getPayeeTypeCode()))));
        }

        if (input.getCareTeam() != null) {
            for (int i = 0; i < input.getCareTeam().size(); i++) {
                ClaimBundleInput.CareTeamMember member = input.getCareTeam().get(i);
                String roleCode = member.getRoleCode() != null ? member.getRoleCode() : "primary";
                Claim.CareTeamComponent ct = new Claim.CareTeamComponent();
                ct.setSequence(member.getSequence());
                ct.setProvider(new Reference(practRoleUrls.get(i)));
                ct.setRole(new CodeableConcept().addCoding(new Coding()
                        .setSystem(NphiesProfiles.CS_CARE_TEAM_ROLE)
                        .setCode(roleCode)));
                if (member.getQualification() != null) {
                    ct.setQualification(new CodeableConcept().addCoding(new Coding()
                            .setSystem(NphiesProfiles.CS_PRACTICE_CODES)
                            .setCode(member.getQualification())));
                }
                claim.addCareTeam(ct);
            }
        }

        if (input.getDiagnoses() != null) {
            for (ClaimBundleInput.DiagnosisEntry diag : input.getDiagnoses()) {
                Claim.DiagnosisComponent dc = new Claim.DiagnosisComponent();
                dc.setSequence(diag.getSequence());
                if (diag.getOnAdmissionCode() != null) {
                    dc.setOnAdmission(new CodeableConcept().addCoding(new Coding()
                            .setSystem(NphiesProfiles.CS_DIAGNOSIS_ON_ADMISSION)
                            .setCode(diag.getOnAdmissionCode())));
                }
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

        Claim.InsuranceComponent insurance = new Claim.InsuranceComponent();
        insurance.setSequence(1);
        insurance.setFocal(true);
        insurance.setIdentifier(new Identifier().setSystem(identSystem).setValue(id));
        insurance.setCoverage(new Reference(coverageUrl));
        claim.addInsurance(insurance);

        if (input.getSupportingInfo() != null) {
            for (ClaimBundleInput.SupportingInfoEntry si : input.getSupportingInfo()) {
                Claim.SupportingInformationComponent sic = new Claim.SupportingInformationComponent();
                sic.setSequence(si.getSequence());
                sic.getCategory().addCoding()
                        .setSystem(NphiesProfiles.CS_CLAIM_INFO_CATEGORY)
                        .setCode(si.getCategoryCode());
                if (si.getQuantityValue() != null) {
                    sic.setValue(new Quantity()
                            .setValue(si.getQuantityValue())
                            .setSystem(NphiesProfiles.CS_UCUM)
                            .setCode(si.getQuantityUnit()));
                } else if (si.getAttachmentData() != null) {
                    Attachment attachment = new Attachment();
                    if (si.getAttachmentContentType() != null) attachment.setContentType(si.getAttachmentContentType());
                    if (si.getAttachmentTitle() != null) attachment.setTitle(si.getAttachmentTitle());
                    attachment.setDataElement(new Base64BinaryType(si.getAttachmentData()));
                    if (si.getAttachmentCreation() != null) attachment.setCreation(toDate(si.getAttachmentCreation()));
                    sic.setValue(attachment);
                } else if (si.getTimingDate() != null) {
                    sic.setTiming(new DateType(toDate(si.getTimingDate())));
                }
                claim.addSupportingInfo(sic);
            }
        }

        String cur = input.getCurrency();
        if (input.getItems() != null) {
            for (ClaimBundleInput.ClaimItemEntry itemEntry : input.getItems()) {
                Claim.ItemComponent item = new Claim.ItemComponent();
                item.setSequence(itemEntry.getSequence());

                if (itemEntry.getTaxAmount() != null) {
                    item.addExtension().setUrl(NphiesProfiles.EXT_TAX)
                            .setValue(new Money().setValue(itemEntry.getTaxAmount()).setCurrency(cur));
                }
                if (itemEntry.getPatientShareAmount() != null) {
                    item.addExtension().setUrl(NphiesProfiles.EXT_PATIENT_SHARE)
                            .setValue(new Money().setValue(itemEntry.getPatientShareAmount()).setCurrency(cur));
                }
                if (itemEntry.getPayerShareAmount() != null) {
                    item.addExtension().setUrl(NphiesProfiles.EXT_PAYER_SHARE)
                            .setValue(new Money().setValue(itemEntry.getPayerShareAmount()).setCurrency(cur));
                }
                if (Boolean.TRUE.equals(itemEntry.getIsPackage())) {
                    item.addExtension().setUrl(NphiesProfiles.EXT_PACKAGE)
                            .setValue(new BooleanType(true));
                }
                if (itemEntry.getPatientInvoiceValue() != null) {
                    item.addExtension().setUrl(NphiesProfiles.EXT_PATIENT_INVOICE)
                            .setValue(new Identifier()
                                    .setSystem(itemEntry.getPatientInvoiceSystem())
                                    .setValue(itemEntry.getPatientInvoiceValue()));
                }

                if (itemEntry.getCareTeamSeqs() != null) {
                    for (int seq : itemEntry.getCareTeamSeqs()) item.addCareTeamSequence(seq);
                }
                if (itemEntry.getDiagnosisSeqs() != null) {
                    for (int seq : itemEntry.getDiagnosisSeqs()) item.addDiagnosisSequence(seq);
                }

                String productSystem = itemEntry.getProductSystem() != null
                        ? itemEntry.getProductSystem() : NphiesProfiles.CS_PROCEDURE;
                Coding productCoding = new Coding().setSystem(productSystem).setCode(itemEntry.getProductCode());
                if (itemEntry.getProductDisplay() != null) productCoding.setDisplay(itemEntry.getProductDisplay());
                item.setProductOrService(new CodeableConcept().addCoding(productCoding));

                if (itemEntry.getServicedPeriodStart() != null) {
                    Period servicedPeriod = new Period();
                    servicedPeriod.setStart(toDate(itemEntry.getServicedPeriodStart()));
                    if (itemEntry.getServicedPeriodEnd() != null) {
                        servicedPeriod.setEnd(toDate(itemEntry.getServicedPeriodEnd()));
                    }
                    item.setServiced(servicedPeriod);
                } else if (itemEntry.getServicedDate() != null) {
                    item.setServiced(new DateType(toDate(itemEntry.getServicedDate())));
                }

                BigDecimal qty = itemEntry.getQty() != null ? itemEntry.getQty() : BigDecimal.ONE;
                item.setQuantity(new Quantity().setValue(qty));
                item.setUnitPrice(new Money().setValue(itemEntry.getUnitPrice()).setCurrency(cur));
                item.setNet(new Money().setValue(itemEntry.getNet()).setCurrency(cur));

                if (itemEntry.getDetail() != null) {
                    for (ClaimBundleInput.ItemDetail d : itemEntry.getDetail()) {
                        Claim.DetailComponent detail = new Claim.DetailComponent();
                        detail.setSequence(d.getSequence());

                        if (d.getTaxAmount() != null) {
                            detail.addExtension().setUrl(NphiesProfiles.EXT_TAX)
                                    .setValue(new Money().setValue(d.getTaxAmount()).setCurrency(cur));
                        }
                        if (d.getPatientShareAmount() != null) {
                            detail.addExtension().setUrl(NphiesProfiles.EXT_PATIENT_SHARE)
                                    .setValue(new Money().setValue(d.getPatientShareAmount()).setCurrency(cur));
                        }
                        if (d.getPayerShareAmount() != null) {
                            detail.addExtension().setUrl(NphiesProfiles.EXT_PAYER_SHARE)
                                    .setValue(new Money().setValue(d.getPayerShareAmount()).setCurrency(cur));
                        }

                        Coding dc = new Coding().setSystem(d.getProductSystem()).setCode(d.getProductCode());
                        if (d.getProductDisplay() != null) dc.setDisplay(d.getProductDisplay());
                        detail.setProductOrService(new CodeableConcept().addCoding(dc));

                        detail.setQuantity(new Quantity().setValue(d.getQuantity()));
                        detail.setUnitPrice(new Money().setValue(d.getUnitPrice()).setCurrency(cur));
                        BigDecimal factor = d.getFactor() != null ? d.getFactor() : BigDecimal.ONE;
                        detail.setFactor(factor);
                        detail.setNet(new Money().setValue(d.getNet()).setCurrency(cur));

                        item.addDetail(detail);
                    }
                }

                claim.addItem(item);
            }
        }

        if (input.getTotalAmount() != null) {
            claim.setTotal(new Money().setValue(input.getTotalAmount()).setCurrency(cur));
        }

        return claim;
    }

    private Patient buildPatient(String id, String providerOrgUrl, ClaimBundleInput input) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.PATIENT));
        patient.setActive(true);

        String nationalId = input.getPatientNationalId().trim();
        boolean isSaudi   = nationalId.startsWith("1");
        String idSystem   = isSaudi ? NphiesProfiles.SYSTEM_NATIONAL_ID : NphiesProfiles.SYSTEM_IQAMA;
        String idTypeCode = isSaudi ? "NI" : "PRC";

        Identifier ident = patient.addIdentifier();
        ident.getType().addCoding()
                .setSystem(NphiesProfiles.CS_V2_0203)
                .setCode(idTypeCode);
        ident.setSystem(idSystem).setValue(nationalId);

        patient.setManagingOrganization(new Reference(providerOrgUrl));

        List<String> givenNames = (input.getPatientGivenNames() != null && !input.getPatientGivenNames().isEmpty())
                ? input.getPatientGivenNames()
                : List.of(input.getPatientFirstName());
        HumanName name = patient.addName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        name.setFamily(input.getPatientFamilyName());
        givenNames.forEach(name::addGiven);
        name.setText(String.join(" ", givenNames) + " " + input.getPatientFamilyName());

        patient.setGender(Enumerations.AdministrativeGender.fromCode(input.getPatientGender()));
        patient.getGenderElement().addExtension()
                .setUrl(NphiesProfiles.EXT_KSA_ADMIN_GENDER)
                .setValue(new CodeableConcept().addCoding(new Coding()
                        .setSystem(NphiesProfiles.CS_KSA_ADMIN_GENDER)
                        .setCode(input.getPatientGender())));

        patient.setBirthDate(toDate(input.getPatientDob()));

        if (input.getPatientPhone() != null) {
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(input.getPatientPhone());
        }

        return patient;
    }

    private Coverage buildCoverage(String id, String patientUrl, String insurerOrgUrl,
                                   ClaimBundleInput input) {
        Coverage coverage = new Coverage();
        coverage.setId(id);
        coverage.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.COVERAGE));
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);

        coverage.addIdentifier()
                .setSystem(NphiesProfiles.SYSTEM_MEMBER_ID)
                .setValue(input.getMemberId());

        coverage.setType(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_COVERAGE_TYPE)
                .setCode(input.getCoverageType())
                .setDisplay(input.getCoverageTypeDisplay())));

        coverage.setSubscriber(new Reference(patientUrl));
        coverage.setSubscriberId(input.getMemberId());
        coverage.setBeneficiary(new Reference(patientUrl));

        coverage.setRelationship(new CodeableConcept().addCoding(new Coding()
                .setSystem(NphiesProfiles.CS_RELATIONSHIP)
                .setCode(input.getCoverageRelationship())));

        if (input.getCoveragePeriodStart() != null || input.getCoveragePeriodEnd() != null) {
            Period period = new Period();
            if (input.getCoveragePeriodStart() != null) period.setStart(toDate(input.getCoveragePeriodStart()));
            if (input.getCoveragePeriodEnd() != null)   period.setEnd(toDate(input.getCoveragePeriodEnd()));
            coverage.setPeriod(period);
        }

        coverage.addPayor(new Reference(insurerOrgUrl));
        return coverage;
    }

    private Organization buildProviderOrg(String id, ClaimBundleInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.PROVIDER_ORGANIZATION));
        org.setActive(true);
        org.setName(input.getProviderName());
        org.addIdentifier()
                .setUse(Identifier.IdentifierUse.OFFICIAL)
                .setSystem(NphiesProfiles.SYSTEM_PROVIDER_LICENSE)
                .setValue(input.getProviderLicenseNo());
        org.addType().addCoding()
                .setSystem(NphiesProfiles.CS_ORG_TYPE)
                .setCode("prov");
        return org;
    }

    private Organization buildInsurerOrg(String id, ClaimBundleInput input) {
        Organization org = new Organization();
        org.setId(id);
        org.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.INSURER_ORGANIZATION));
        org.setActive(true);
        org.setName(input.getPayerName());
        org.addIdentifier()
                .setUse(Identifier.IdentifierUse.OFFICIAL)
                .setSystem(NphiesProfiles.SYSTEM_PAYER_LICENSE)
                .setValue(input.getPayerLicenseNo());
        org.addType().addCoding()
                .setSystem(NphiesProfiles.CS_ORG_TYPE)
                .setCode("ins");
        return org;
    }

    private PractitionerRole buildPractitionerRole(String id, String providerOrgUrl,
                                                    ClaimBundleInput.CareTeamMember member) {
        PractitionerRole role = new PractitionerRole();
        role.setId(id);
        role.getMeta().addProfile(NphiesProfiles.versioned(NphiesProfiles.PRACTITIONER_ROLE));
        role.setActive(member.isActive());

        String roleCode = member.getRoleCode() != null ? member.getRoleCode() : "primary";

        role.addIdentifier()
                .setSystem(NphiesProfiles.CS_PRACTITIONER_ROLE)
                .setValue(roleCode);

        Reference practRef = new Reference();
        practRef.setType("Practitioner");
        practRef.setIdentifier(new Identifier()
                .setSystem(NphiesProfiles.SYSTEM_PRACTITIONER_LICENSE)
                .setValue(member.getPractitionerLicense()));
        role.setPractitioner(practRef);

        role.setOrganization(new Reference(providerOrgUrl));

        role.addCode().addCoding()
                .setSystem(NphiesProfiles.CS_PRACTITIONER_ROLE)
                .setCode(roleCode);

        if (member.getQualification() != null) {
            role.addSpecialty().addCoding()
                    .setSystem(NphiesProfiles.CS_PRACTICE_CODES)
                    .setCode(member.getQualification());
        }

        return role;
    }

    private Bundle.BundleEntryComponent entry(String fullUrl, Resource resource) {
        return new Bundle.BundleEntryComponent().setFullUrl(fullUrl).setResource(resource);
    }

    private String url(String base, String resourceType, String id) {
        return base + "/" + resourceType + "/" + id;
    }

    private Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
