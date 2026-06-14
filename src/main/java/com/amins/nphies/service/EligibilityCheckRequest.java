package com.amins.nphies.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class EligibilityCheckRequest {

    // ── Request identity ─────────────────────────────────────────────────────
    @NonNull String    requestId;

    /**
     * Provider system base URL for FHIR resource fullUrls (e.g. "http://provider.com").
     */
    @Builder.Default String providerBaseUrl = "http://provider.com";

    // ── Patient ──────────────────────────────────────────────────────────────
    @NonNull String    patientNationalId;
    @NonNull String    patientFirstName;
    /** Additional given / middle names. Optional. */
    List<String>       patientGivenNames;
    @NonNull String    patientFamilyName;
    @NonNull LocalDate patientDateOfBirth;
    @NonNull String    patientGender;            // male|female|other|unknown
    /** Patient phone number. Optional. */
    String             patientPhone;
    /** HL7 v3 marital status code (M, S, D, W, …). Optional. */
    String             patientMaritalStatus;
    /** ISO 3166 alpha-3 nationality code for Iqama holders (e.g. "PSE"). Optional. */
    String             patientNationalityCode;
    String             patientNationalityDisplay;

    // ── Coverage / insurance ─────────────────────────────────────────────────
    @NonNull String    memberId;
    /** Payer-specific member ID identifier system URL. Optional. */
    String             memberIdSystem;
    @Builder.Default String coverageRelationship = "self";
    /** NPHIES coverage type code (e.g. EHCPOL). */
    @Builder.Default String coverageType        = "EHCPOL";
    @Builder.Default String coverageTypeDisplay = "extended healthcare";
    LocalDate          coveragePeriodStart;
    LocalDate          coveragePeriodEnd;
    /** Insurance business arrangement / contract reference. Optional. */
    String             businessArrangement;

    // ── Payer ────────────────────────────────────────────────────────────────
    @NonNull String    payerLicenseNumber;
    @NonNull String    payerName;

    // ── Request parameters ───────────────────────────────────────────────────
    LocalDate          servicedDate;             // null → today
    LocalDate          servicedPeriodEnd;        // null → same as servicedDate
    @Builder.Default List<String> purposes = List.of("benefits");
}
