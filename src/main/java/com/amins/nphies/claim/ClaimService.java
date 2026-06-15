package com.amins.nphies.claim;

import com.amins.nphies.beneficiary.Beneficiary;
import com.amins.nphies.beneficiary.BeneficiaryRepository;
import com.amins.nphies.claim.dto.*;
import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.coverage.Coverage;
import com.amins.nphies.coverage.CoverageRepository;
import com.amins.nphies.encounter.Encounter;
import com.amins.nphies.encounter.EncounterRepository;
import com.amins.nphies.exception.NphiesException;
import com.amins.nphies.fhir.bundle.ClaimBundleBuilder;
import com.amins.nphies.fhir.bundle.ClaimBundleInput;
import com.amins.nphies.fhir.response.ClaimResponseMapper;
import com.amins.nphies.gateway.NphiesGatewayClient;
import com.amins.nphies.model.TenantContext;
import com.amins.nphies.organization.Organization;
import com.amins.nphies.organization.OrganizationRepository;
import com.amins.nphies.practitioner.Practitioner;
import com.amins.nphies.practitioner.PractitionerRepository;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final TenantNphiesConfigRepository configRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final CoverageRepository coverageRepository;
    private final EncounterRepository encounterRepository;
    private final OrganizationRepository organizationRepository;
    private final PractitionerRepository practitionerRepository;
    private final ClaimRepository claimRepository;
    private final ClaimItemRepository claimItemRepository;
    private final ClaimCareTeamRepository claimCareTeamRepository;
    private final ClaimDiagnosisRepository claimDiagnosisRepository;
    private final ClaimResponseRepository claimResponseRepository;
    private final ClaimBundleBuilder claimBundleBuilder;
    private final NphiesGatewayClient gatewayClient;
    private final ClaimResponseMapper claimResponseMapper;

    @Transactional
    public ClaimSummaryResponse submitClaim(ClaimRequest req) {
        String tenantId = TenantContext.require();
        log.info("Submitting claim for tenant: {}", tenantId);

        TenantNphiesConfig config = configRepository.findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new NphiesException("No active NPHIES configuration for tenant: " + tenantId));

        Beneficiary beneficiary = beneficiaryRepository.findByIdAndActiveTrue(req.getBeneficiaryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));

        Coverage coverage = coverageRepository.findByIdAndActiveTrue(req.getCoverageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coverage not found"));

        Organization insurerOrg = organizationRepository.findByIdAndActiveTrue(req.getInsurerOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insurer organization not found"));

        // Persist Claim entity (PENDING status)
        String claimId = UUID.randomUUID().toString();
        Claim claim = new Claim();
        claim.setClaimId(claimId);
        claim.setUseType(req.getUseType() != null ? req.getUseType() : Claim.UseType.CLAIM);
        claim.setClaimType(req.getClaimType() != null ? req.getClaimType() : Claim.ClaimType.INSTITUTIONAL);
        claim.setPriority(req.getPriority() != null ? req.getPriority() : "normal");
        claim.setBeneficiaryId(req.getBeneficiaryId());
        claim.setCoverageId(req.getCoverageId());
        claim.setEncounterId(req.getEncounterId());
        claim.setInsurerOrgId(req.getInsurerOrgId());
        claim.setBillablePeriodStart(req.getBillablePeriodStart());
        claim.setBillablePeriodEnd(req.getBillablePeriodEnd());
        claim.setTotalNet(req.getTotalNet());
        claim.setTotalGross(req.getTotalGross());
        claim.setCurrency(req.getCurrency() != null ? req.getCurrency() : "SAR");
        claim.setSubmissionStatus(Claim.SubmissionStatus.PENDING);
        claim = claimRepository.save(claim);

        // Persist care team
        List<ClaimCareTeam> careTeamEntities = new ArrayList<>();
        if (req.getCareTeam() != null) {
            for (ClaimRequest.CareTeamEntry entry : req.getCareTeam()) {
                ClaimCareTeam ct = new ClaimCareTeam();
                ct.setClaimId(claim.getId());
                ct.setSequence(entry.getSequence());
                ct.setPractitionerId(entry.getPractitionerId());
                ct.setRoleCode(entry.getRoleCode() != null ? entry.getRoleCode() : "primary");
                ct.setQualification(entry.getQualification());
                careTeamEntities.add(claimCareTeamRepository.save(ct));
            }
        }

        // Persist diagnoses
        if (req.getDiagnoses() != null) {
            for (ClaimRequest.DiagnosisEntry entry : req.getDiagnoses()) {
                ClaimDiagnosis diag = new ClaimDiagnosis();
                diag.setClaimId(claim.getId());
                diag.setSequence(entry.getSequence());
                diag.setIcd10Code(entry.getIcd10Code());
                diag.setIcd10Display(entry.getIcd10Display());
                diag.setDiagnosisType(entry.getDiagnosisType() != null ? entry.getDiagnosisType() : "principal");
                diag.setOnAdmission(entry.getOnAdmission());
                claimDiagnosisRepository.save(diag);
            }
        }

        // Persist items
        if (req.getItems() != null) {
            for (ClaimRequest.ClaimItemDto itemDto : req.getItems()) {
                ClaimItem item = new ClaimItem();
                item.setClaimId(claim.getId());
                item.setSequence(itemDto.getSequence());
                item.setCareTeamSequences(itemDto.getCareTeamSequences());
                item.setDiagnosisSequences(itemDto.getDiagnosisSequences());
                item.setProductServiceCode(itemDto.getProductServiceCode());
                item.setProductServiceSystem(itemDto.getProductServiceSystem());
                item.setServicedDate(itemDto.getServicedDate());
                item.setQuantity(itemDto.getQuantity() != null ? itemDto.getQuantity() : java.math.BigDecimal.ONE);
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setNetAmount(itemDto.getNetAmount());
                item.setBodySiteCode(itemDto.getBodySiteCode());
                item.setModifierCodes(itemDto.getModifierCodes());
                claimItemRepository.save(item);
            }
        }

        // Build FHIR bundle
        ClaimBundleInput bundleInput = buildBundleInput(claim, claimId, config, beneficiary,
                coverage, insurerOrg, req, careTeamEntities);
        String bundleJson = claimBundleBuilder.build(bundleInput);

        // Submit to NPHIES
        String responseJson;
        try {
            responseJson = gatewayClient.submitBundle(tenantId, config.getApiBaseUrl(), bundleJson);
            claim.setSubmissionStatus(Claim.SubmissionStatus.SUBMITTED);
            claim.setSubmittedAt(OffsetDateTime.now());
        } catch (Exception e) {
            log.error("Failed to submit claim to NPHIES for tenant: {}, claimId: {}", tenantId, claimId, e);
            claim.setSubmissionStatus(Claim.SubmissionStatus.ERROR);
            claimRepository.save(claim);
            // Persist error response
            persistClaimResponse(claim, null,
                    ClaimResponseDto.builder()
                            .outcome("error")
                            .disposition(e.getMessage())
                            .receivedAt(OffsetDateTime.now())
                            .currency(claim.getCurrency())
                            .build());
            return new ClaimSummaryResponse(claim);
        }

        // Map and persist NPHIES response
        ClaimResponseDto responseDto = claimResponseMapper.map(claimId, responseJson);
        if (responseDto.getBundleId() != null) {
            claim.setNphiesBundleId(responseDto.getBundleId());
        }
        persistClaimResponse(claim, responseJson, responseDto);

        claimRepository.save(claim);
        log.info("Claim submitted successfully for tenant: {}, claimId: {}", tenantId, claimId);
        return new ClaimSummaryResponse(claim);
    }

    @Transactional(readOnly = true)
    public ClaimDetailResponse getClaimDetail(String claimId) {
        Claim claim = claimRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));

        List<ClaimCareTeam> careTeam = claimCareTeamRepository.findAllByClaimIdOrderBySequence(claim.getId());
        List<ClaimDiagnosis> diagnoses = claimDiagnosisRepository.findAllByClaimIdOrderBySequence(claim.getId());
        List<ClaimItem> items = claimItemRepository.findAllByClaimIdOrderBySequence(claim.getId());

        ClaimResponseDto latestResponse = claimResponseRepository
                .findTopByClaimIdOrderByReceivedAtDesc(claim.getId())
                .map(this::mapResponseEntityToDto)
                .orElse(null);

        return new ClaimDetailResponse(claim, careTeam, diagnoses, items, latestResponse);
    }

    @Transactional(readOnly = true)
    public List<ClaimSummaryResponse> listClaims(Claim.SubmissionStatus status) {
        List<Claim> claims = status != null
                ? claimRepository.findAllBySubmissionStatus(status)
                : claimRepository.findAll();
        return claims.stream().map(ClaimSummaryResponse::new).toList();
    }

    @Transactional
    public ClaimResponseDto pollClaimResponse(String claimId) {
        String tenantId = TenantContext.require();
        Claim claim = claimRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));

        if (claim.getNphiesBundleId() == null || claim.getNphiesBundleId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Claim has no NPHIES bundle ID to poll");
        }

        TenantNphiesConfig config = configRepository.findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new NphiesException("No active NPHIES configuration for tenant: " + tenantId));

        String responseJson = gatewayClient.pollBundleResponse(tenantId, config.getApiBaseUrl(), claim.getNphiesBundleId());
        ClaimResponseDto responseDto = claimResponseMapper.map(claimId, responseJson);
        persistClaimResponse(claim, responseJson, responseDto);

        return responseDto;
    }

    private ClaimBundleInput buildBundleInput(Claim claim, String claimId,
                                               TenantNphiesConfig config,
                                               Beneficiary beneficiary,
                                               Coverage coverage,
                                               Organization insurerOrg,
                                               ClaimRequest req,
                                               List<ClaimCareTeam> careTeamEntities) {
        List<ClaimBundleInput.CareTeamMember> careTeamMembers = new ArrayList<>();
        if (req.getCareTeam() != null) {
            for (int i = 0; i < req.getCareTeam().size(); i++) {
                ClaimRequest.CareTeamEntry entry = req.getCareTeam().get(i);
                Practitioner practitioner = practitionerRepository.findById(entry.getPractitionerId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Practitioner not found: " + entry.getPractitionerId()));
                careTeamMembers.add(ClaimBundleInput.CareTeamMember.builder()
                        .sequence(entry.getSequence())
                        .practitionerLicense(practitioner.getPractitionerLicense())
                        .firstName(practitioner.getFirstName())
                        .familyName(practitioner.getFamilyName())
                        .roleCode(entry.getRoleCode())
                        .qualification(entry.getQualification())
                        .build());
            }
        }

        List<ClaimBundleInput.DiagnosisEntry> diagnosisEntries = new ArrayList<>();
        if (req.getDiagnoses() != null) {
            for (ClaimRequest.DiagnosisEntry entry : req.getDiagnoses()) {
                diagnosisEntries.add(ClaimBundleInput.DiagnosisEntry.builder()
                        .sequence(entry.getSequence())
                        .icd10Code(entry.getIcd10Code())
                        .icd10Display(entry.getIcd10Display())
                        .diagnosisType(entry.getDiagnosisType())
                        .onAdmissionCode(entry.getOnAdmission())
                        .build());
            }
        }

        List<ClaimBundleInput.ClaimItemEntry> itemEntries = new ArrayList<>();
        if (req.getItems() != null) {
            for (ClaimRequest.ClaimItemDto itemDto : req.getItems()) {
                List<ClaimBundleInput.ItemDetail> detailList = null;
                if (itemDto.getDetail() != null) {
                    detailList = new ArrayList<>();
                    for (ClaimRequest.ItemDetailDto d : itemDto.getDetail()) {
                        detailList.add(ClaimBundleInput.ItemDetail.builder()
                                .sequence(d.getSequence())
                                .productCode(d.getProductCode())
                                .productSystem(d.getProductSystem())
                                .productDisplay(d.getProductDisplay())
                                .additionalProductCodings(toAdditionalCodings(d.getAdditionalProductCodings()))
                                .quantity(d.getQuantity() != null ? d.getQuantity() : java.math.BigDecimal.ONE)
                                .unitPrice(d.getUnitPrice())
                                .factor(d.getFactor())
                                .net(d.getNet())
                                .taxAmount(d.getTaxAmount())
                                .patientShareAmount(d.getPatientShareAmount())
                                .payerShareAmount(d.getPayerShareAmount())
                                .build());
                    }
                }
                itemEntries.add(ClaimBundleInput.ClaimItemEntry.builder()
                        .sequence(itemDto.getSequence())
                        .careTeamSeqs(itemDto.getCareTeamSequences())
                        .diagnosisSeqs(itemDto.getDiagnosisSequences())
                        .productCode(itemDto.getProductServiceCode())
                        .productSystem(itemDto.getProductServiceSystem())
                        .productDisplay(itemDto.getProductServiceDisplay())
                        .additionalProductCodings(toAdditionalCodings(itemDto.getAdditionalProductCodings()))
                        .servicedDate(itemDto.getServicedDate())
                        .servicedPeriodStart(itemDto.getServicedPeriodStart())
                        .servicedPeriodEnd(itemDto.getServicedPeriodEnd())
                        .qty(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .factor(itemDto.getFactor())
                        .net(itemDto.getNetAmount())
                        .bodySite(itemDto.getBodySiteCode())
                        .bodySiteSystem(itemDto.getBodySiteSystem())
                        .bodySiteDisplay(itemDto.getBodySiteDisplay())
                        .modifiers(itemDto.getModifierCodes())
                        .taxAmount(itemDto.getTaxAmount())
                        .patientShareAmount(itemDto.getPatientShareAmount())
                        .payerShareAmount(itemDto.getPayerShareAmount())
                        .isPackage(itemDto.getIsPackage())
                        .patientInvoiceSystem(itemDto.getPatientInvoiceSystem())
                        .patientInvoiceValue(itemDto.getPatientInvoiceValue())
                        .detail(detailList)
                        .build());
            }
        }

        List<ClaimBundleInput.SupportingInfoEntry> siEntries = new ArrayList<>();
        if (req.getSupportingInfo() != null) {
            for (ClaimRequest.SupportingInfoDto si : req.getSupportingInfo()) {
                siEntries.add(ClaimBundleInput.SupportingInfoEntry.builder()
                        .sequence(si.getSequence())
                        .categoryCode(si.getCategoryCode())
                        .quantityValue(si.getQuantityValue())
                        .quantityUnit(si.getQuantityUnit())
                        .timingDate(si.getTimingDate())
                        .timingPeriodStart(si.getTimingPeriodStart())
                        .timingPeriodEnd(si.getTimingPeriodEnd())
                        .valueString(si.getValueString())
                        .attachmentContentType(si.getAttachmentContentType())
                        .attachmentTitle(si.getAttachmentTitle())
                        .attachmentData(si.getAttachmentData())
                        .attachmentCreation(si.getAttachmentCreation())
                        .build());
            }
        }

        return ClaimBundleInput.builder()
                .requestId(claimId)
                .useType(claim.getUseType().toFhirCode())
                .claimType(claim.getClaimType().toFhirCode())
                .claimSubType(req.getClaimSubType())
                .episodeSystem(req.getEpisodeSystem())
                .episodeValue(req.getEpisodeValue())
                .eligibilityOfflineReference(req.getEligibilityOfflineReference())
                .eligibilityOfflineDate(req.getEligibilityOfflineDate())
                .coverageClassCode(req.getCoverageClassCode())
                .coverageClassValue(req.getCoverageClassValue())
                .priority(claim.getPriority())
                .patientNationalId(beneficiary.getNationalId())
                .patientFirstName(beneficiary.getFirstName())
                .patientFamilyName(beneficiary.getFamilyName())
                .patientDob(beneficiary.getDateOfBirth())
                .patientGender(beneficiary.getGender())
                .patientPhone(beneficiary.getPhone())
                .memberId(coverage.getMemberId())
                .coverageRelationship(coverage.getCoverageRelationship())
                .coveragePeriodStart(coverage.getPeriodStart())
                .coveragePeriodEnd(coverage.getPeriodEnd())
                .payerLicenseNo(coverage.getPayerLicenseNo())
                .payerName(coverage.getPayerName())
                .providerLicenseNo(config.getProviderLicenseNo())
                .providerName(config.getTenantId())
                .billablePeriodStart(claim.getBillablePeriodStart())
                .billablePeriodEnd(claim.getBillablePeriodEnd())
                .currency(claim.getCurrency())
                .totalAmount(claim.getTotalNet())
                .careTeam(careTeamMembers)
                .diagnoses(diagnosisEntries)
                .items(itemEntries)
                .supportingInfo(siEntries.isEmpty() ? null : siEntries)
                .build();
    }

    private List<ClaimBundleInput.AdditionalCoding> toAdditionalCodings(
            List<ClaimRequest.AdditionalCodingDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return null;
        List<ClaimBundleInput.AdditionalCoding> result = new ArrayList<>();
        for (ClaimRequest.AdditionalCodingDto d : dtos) {
            result.add(ClaimBundleInput.AdditionalCoding.builder()
                    .system(d.getSystem())
                    .code(d.getCode())
                    .display(d.getDisplay())
                    .build());
        }
        return result;
    }

    private void persistClaimResponse(Claim claim, String rawJson, ClaimResponseDto dto) {
        ClaimResponseEntity responseEntity = new ClaimResponseEntity();
        responseEntity.setClaimId(claim.getId());
        responseEntity.setOutcome(dto.getOutcome());
        responseEntity.setDisposition(dto.getDisposition());
        responseEntity.setTotalBenefit(dto.getTotalBenefit());
        responseEntity.setTotalSubmitted(dto.getTotalSubmitted());
        responseEntity.setPaymentAmount(dto.getPaymentAmount());
        responseEntity.setPaymentDate(dto.getPaymentDate());
        responseEntity.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : claim.getCurrency());
        responseEntity.setRawResponseJson(rawJson);
        responseEntity.setReceivedAt(dto.getReceivedAt() != null ? dto.getReceivedAt() : OffsetDateTime.now());
        claimResponseRepository.save(responseEntity);
    }

    private ClaimResponseDto mapResponseEntityToDto(ClaimResponseEntity entity) {
        return ClaimResponseDto.builder()
                .outcome(entity.getOutcome())
                .disposition(entity.getDisposition())
                .totalBenefit(entity.getTotalBenefit())
                .totalSubmitted(entity.getTotalSubmitted())
                .paymentAmount(entity.getPaymentAmount())
                .paymentDate(entity.getPaymentDate())
                .currency(entity.getCurrency())
                .receivedAt(entity.getReceivedAt())
                .build();
    }
}
