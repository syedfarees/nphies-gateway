package com.amins.nphies.fhir.bundle;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class ClaimBundleInput {

    @NonNull String requestId;
    @Builder.Default String providerBaseUrl = "http://provider.com";
    @NonNull String useType;
    @NonNull String claimType;
    String claimSubType;
    String claimIdentifierSystem;
    @Builder.Default String priority = "normal";
    @Builder.Default String payeeTypeCode = "provider";

    // Patient
    @NonNull String patientNationalId;
    @NonNull String patientFirstName;
    List<String> patientGivenNames;
    @NonNull String patientFamilyName;
    @NonNull LocalDate patientDob;
    @NonNull String patientGender;
    String patientPhone;

    // Coverage
    @NonNull String memberId;
    @Builder.Default String coverageRelationship = "self";
    @Builder.Default String coverageType = "EHCPOL";
    @Builder.Default String coverageTypeDisplay = "extended healthcare";
    LocalDate coveragePeriodStart;
    LocalDate coveragePeriodEnd;

    // Payer
    @NonNull String payerLicenseNo;
    @NonNull String payerName;

    // Provider (always from tenant config, never from client)
    @NonNull String providerLicenseNo;
    @NonNull String providerName;

    // Billable period
    @NonNull LocalDate billablePeriodStart;
    @NonNull LocalDate billablePeriodEnd;

    @Builder.Default String currency = "SAR";
    BigDecimal totalAmount;

    List<CareTeamMember> careTeam;
    List<DiagnosisEntry> diagnoses;
    List<ClaimItemEntry> items;
    List<SupportingInfoEntry> supportingInfo;

    @Value
    @Builder
    public static class CareTeamMember {
        int sequence;
        @NonNull String practitionerLicense;
        @NonNull String firstName;
        @NonNull String familyName;
        @Builder.Default String roleCode = "primary";
        String qualification;
    }

    @Value
    @Builder
    public static class DiagnosisEntry {
        int sequence;
        @NonNull String icd10Code;
        String icd10Display;
        @Builder.Default String diagnosisType = "principal";
        String onAdmissionCode;
    }

    @Value
    @Builder
    public static class ClaimItemEntry {
        int sequence;
        Integer[] careTeamSeqs;
        Integer[] diagnosisSeqs;
        @NonNull String productCode;
        String productSystem;
        String productDisplay;
        @NonNull LocalDate servicedDate;
        @Builder.Default BigDecimal qty = BigDecimal.ONE;
        @NonNull BigDecimal unitPrice;
        @NonNull BigDecimal net;
        String bodySite;
        String[] modifiers;
        BigDecimal taxAmount;
        BigDecimal patientShareAmount;
        Boolean isPackage;
        List<ItemDetail> detail;
    }

    @Value
    @Builder
    public static class ItemDetail {
        int sequence;
        @NonNull String productCode;
        @NonNull String productSystem;
        String productDisplay;
        @Builder.Default BigDecimal quantity = BigDecimal.ONE;
        @NonNull BigDecimal unitPrice;
        @Builder.Default BigDecimal factor = BigDecimal.ONE;
        @NonNull BigDecimal net;
        BigDecimal taxAmount;
        BigDecimal patientShareAmount;
        BigDecimal payerShareAmount;
    }

    @Value
    @Builder
    public static class SupportingInfoEntry {
        int sequence;
        @NonNull String categoryCode;
        BigDecimal quantityValue;
        String quantityUnit;
        LocalDate timingDate;
    }
}
