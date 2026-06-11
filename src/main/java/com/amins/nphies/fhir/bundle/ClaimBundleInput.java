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
    @NonNull String useType;
    @NonNull String claimType;
    String priority;

    // Patient
    @NonNull String patientNationalId;
    @NonNull String patientFirstName;
    @NonNull String patientFamilyName;
    @NonNull java.time.LocalDate patientDob;
    @NonNull String patientGender;
    String patientIdType;

    // Coverage
    @NonNull String memberId;
    String coverageRelationship;

    // Payer
    @NonNull String payerLicenseNo;
    @NonNull String payerName;

    // Provider (from tenant config)
    @NonNull String providerLicenseNo;
    @NonNull String providerName;

    // Billable period
    @NonNull LocalDate billablePeriodStart;
    @NonNull LocalDate billablePeriodEnd;

    String currency;

    // Care team
    List<CareTeamMember> careTeam;

    // Diagnoses
    List<DiagnosisEntry> diagnoses;

    // Items
    List<ClaimItemEntry> items;

    @Value
    @Builder
    public static class CareTeamMember {
        int sequence;
        @NonNull String practitionerLicense;
        @NonNull String firstName;
        @NonNull String familyName;
        String roleCode;
        String qualification;
    }

    @Value
    @Builder
    public static class DiagnosisEntry {
        int sequence;
        @NonNull String icd10Code;
        String icd10Display;
        String diagnosisType;
    }

    @Value
    @Builder
    public static class ClaimItemEntry {
        int sequence;
        Integer[] careTeamSeqs;
        Integer[] diagnosisSeqs;
        @NonNull String productCode;
        String productSystem;
        @NonNull LocalDate servicedDate;
        BigDecimal qty;
        @NonNull BigDecimal unitPrice;
        @NonNull BigDecimal net;
        String bodySite;
        String[] modifiers;
    }
}
