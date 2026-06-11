package com.amins.nphies.beneficiary.dto;

import com.amins.nphies.beneficiary.Beneficiary;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
public class BeneficiaryResponse {

    private final Long id;
    private final String tenantId;
    private final String nationalId;
    private final Beneficiary.IdType idType;
    private final String firstName;
    private final String familyName;
    private final LocalDate dateOfBirth;
    private final String gender;
    private final String memberId;
    private final String payerLicenseNo;
    private final String payerName;
    private final String coverageRelationship;
    private final String phone;
    private final String email;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public BeneficiaryResponse(Beneficiary b) {
        this.id = b.getId();
        this.tenantId = b.getTenantId();
        this.nationalId = b.getNationalId();
        this.idType = b.getIdType();
        this.firstName = b.getFirstName();
        this.familyName = b.getFamilyName();
        this.dateOfBirth = b.getDateOfBirth();
        this.gender = b.getGender();
        this.memberId = b.getMemberId();
        this.payerLicenseNo = b.getPayerLicenseNo();
        this.payerName = b.getPayerName();
        this.coverageRelationship = b.getCoverageRelationship();
        this.phone = b.getPhone();
        this.email = b.getEmail();
        this.createdAt = b.getCreatedAt();
        this.updatedAt = b.getUpdatedAt();
    }
}
