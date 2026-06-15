package com.amins.nphies.claim.dto;

import com.amins.nphies.claim.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class ClaimDetailResponse {

    private final Long id;
    private final String claimId;
    private final Claim.UseType useType;
    private final Claim.ClaimType claimType;
    private final String priority;
    private final Long beneficiaryId;
    private final Long coverageId;
    private final Long encounterId;
    private final Long insurerOrgId;
    private final LocalDate billablePeriodStart;
    private final LocalDate billablePeriodEnd;
    private final BigDecimal totalNet;
    private final BigDecimal totalGross;
    private final String currency;
    private final Claim.SubmissionStatus submissionStatus;
    private final String nphiesBundleId;
    private final OffsetDateTime submittedAt;
    private final OffsetDateTime createdAt;
    private final List<CareTeamItem> careTeam;
    private final List<DiagnosisItem> diagnoses;
    private final List<ItemEntry> items;
    private final ClaimResponseDto latestResponse;

    public ClaimDetailResponse(Claim c,
                               List<ClaimCareTeam> careTeam,
                               List<ClaimDiagnosis> diagnoses,
                               List<ClaimItem> items,
                               ClaimResponseDto latestResponse) {
        this.id = c.getId();
        this.claimId = c.getClaimId();
        this.useType = c.getUseType();
        this.claimType = c.getClaimType();
        this.priority = c.getPriority();
        this.beneficiaryId = c.getBeneficiaryId();
        this.coverageId = c.getCoverageId();
        this.encounterId = c.getEncounterId();
        this.insurerOrgId = c.getInsurerOrgId();
        this.billablePeriodStart = c.getBillablePeriodStart();
        this.billablePeriodEnd = c.getBillablePeriodEnd();
        this.totalNet = c.getTotalNet();
        this.totalGross = c.getTotalGross();
        this.currency = c.getCurrency();
        this.submissionStatus = c.getSubmissionStatus();
        this.nphiesBundleId = c.getNphiesBundleId();
        this.submittedAt = c.getSubmittedAt();
        this.createdAt = c.getCreatedAt();
        this.careTeam = careTeam.stream().map(CareTeamItem::new).toList();
        this.diagnoses = diagnoses.stream().map(DiagnosisItem::new).toList();
        this.items = items.stream().map(ItemEntry::new).toList();
        this.latestResponse = latestResponse;
    }

    @Getter
    public static class CareTeamItem {
        private final int sequence;
        private final Long practitionerId;
        private final String roleCode;
        private final String qualification;

        public CareTeamItem(ClaimCareTeam ct) {
            this.sequence = ct.getSequence();
            this.practitionerId = ct.getPractitionerId();
            this.roleCode = ct.getRoleCode();
            this.qualification = ct.getQualification();
        }
    }

    @Getter
    public static class DiagnosisItem {
        private final int sequence;
        private final String icd10Code;
        private final String icd10Display;
        private final String diagnosisType;
        private final String onAdmission;

        public DiagnosisItem(ClaimDiagnosis d) {
            this.sequence = d.getSequence();
            this.icd10Code = d.getIcd10Code();
            this.icd10Display = d.getIcd10Display();
            this.diagnosisType = d.getDiagnosisType();
            this.onAdmission = d.getOnAdmission();
        }
    }

    @Getter
    public static class ItemEntry {
        private final int sequence;
        private final String productServiceCode;
        private final LocalDate servicedDate;
        private final java.math.BigDecimal quantity;
        private final java.math.BigDecimal unitPrice;
        private final java.math.BigDecimal netAmount;

        public ItemEntry(ClaimItem i) {
            this.sequence = i.getSequence();
            this.productServiceCode = i.getProductServiceCode();
            this.servicedDate = i.getServicedDate();
            this.quantity = i.getQuantity();
            this.unitPrice = i.getUnitPrice();
            this.netAmount = i.getNetAmount();
        }
    }
}
