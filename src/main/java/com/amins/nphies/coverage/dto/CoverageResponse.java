package com.amins.nphies.coverage.dto;

import com.amins.nphies.coverage.Coverage;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
public class CoverageResponse {

    private final Long id;
    private final String tenantId;
    private final Long beneficiaryId;
    private final String memberId;
    private final String subscriberId;
    private final String payerLicenseNo;
    private final String payerName;
    private final String coverageRelationship;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String classValue;
    private final String className;
    private final int orderOfBenefit;
    private final Coverage.Status status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public CoverageResponse(Coverage c) {
        this.id = c.getId();
        this.tenantId = c.getTenantId();
        this.beneficiaryId = c.getBeneficiaryId();
        this.memberId = c.getMemberId();
        this.subscriberId = c.getSubscriberId();
        this.payerLicenseNo = c.getPayerLicenseNo();
        this.payerName = c.getPayerName();
        this.coverageRelationship = c.getCoverageRelationship();
        this.periodStart = c.getPeriodStart();
        this.periodEnd = c.getPeriodEnd();
        this.classValue = c.getClassValue();
        this.className = c.getClassName();
        this.orderOfBenefit = c.getOrderOfBenefit();
        this.status = c.getStatus();
        this.createdAt = c.getCreatedAt();
        this.updatedAt = c.getUpdatedAt();
    }
}
