package com.amins.nphies.fhir.bundle;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * Caller-supplied data for building a NPHIES CoverageEligibilityRequest Bundle.
 */
@Value
@Builder
public class EligibilityRequestInput {

    // ── Request identity ─────────────────────────────────────────────────────

    /** UUID for the Bundle.id and transaction tracking. */
    @NonNull String requestId;

    /**
     * Provider system base URL used for resource fullUrls and internal references
     * (e.g. "http://provider.com"). MessageHeader uses urn:uuid; all other resources
     * use {providerBaseUrl}/{ResourceType}/{id}.
     */
    @Builder.Default
    String providerBaseUrl = "http://provider.com";

    // ── Patient ──────────────────────────────────────────────────────────────

    /**
     * Saudi national ID (starts with 1) or Iqama number (starts with 2).
     * The builder selects the correct NPHIES identifier system automatically.
     */
    @NonNull String patientNationalId;

    /** Primary given name (used when patientGivenNames is null/empty). */
    @NonNull String patientFirstName;

    /** Additional given names (middle names). When set, overrides patientFirstName. */
    List<String> patientGivenNames;

    @NonNull String patientFamilyName;
    @NonNull LocalDate patientDateOfBirth;

    /** FHIR administrative gender: {@code male}, {@code female}, {@code other}, {@code unknown}. */
    @NonNull String patientGender;

    /** Patient phone number. Optional — omitted from bundle when null. */
    String patientPhone;

    /**
     * HL7 v3 marital status code (M=Married, S=Single, D=Divorced, W=Widowed, …).
     * Optional — omitted from bundle when null.
     */
    String patientMaritalStatus;

    /**
     * ISO 3166 alpha-3 country code for the patient's nationality (e.g. "SAU", "PSE").
     * Required for Iqama holders; added as an extension on the identifier.
     */
    String patientNationalityCode;

    /** Display name for the nationality country (e.g. "Palestine, State of"). Optional. */
    String patientNationalityDisplay;

    // ── Coverage / insurance ─────────────────────────────────────────────────

    /** Member/beneficiary ID issued by the insurer. */
    @NonNull String memberId;

    /**
     * Identifier system URL for the member ID (payer-specific).
     * Defaults to {@link com.amins.nphies.fhir.NphiesProfiles#SYSTEM_MEMBER_ID} when null.
     */
    String memberIdSystem;

    /**
     * Subscriber relationship code per
     * {@code http://terminology.hl7.org/CodeSystem/subscriber-relationship}.
     * Typical values: {@code self}, {@code child}, {@code spouse}.
     */
    @Builder.Default
    String coverageRelationship = "self";

    /**
     * NPHIES coverage type code (e.g. {@code EHCPOL}, {@code PUBLICPOL}).
     */
    @Builder.Default
    String coverageType = "EHCPOL";

    /** Display text for coverageType (e.g. "extended healthcare"). */
    @Builder.Default
    String coverageTypeDisplay = "extended healthcare";

    /** Start date of the coverage period. Optional. */
    LocalDate coveragePeriodStart;

    /** End date of the coverage period. Optional. */
    LocalDate coveragePeriodEnd;

    /**
     * Insurance business arrangement / contract reference number.
     * Optional — omitted from the insurance component when null.
     */
    String businessArrangement;

    // ── Payer (insurer) ──────────────────────────────────────────────────────

    /** CCHI-issued payer license number. */
    @NonNull String payerLicenseNumber;

    @NonNull String payerName;

    // ── Provider ─────────────────────────────────────────────────────────────

    /** CCHI-issued provider license number (sourced from TenantNphiesConfig). */
    @NonNull String providerLicenseNumber;

    @NonNull String providerName;

    // ── Request parameters ───────────────────────────────────────────────────

    /**
     * Start date of the serviced period. Defaults to today if null.
     */
    LocalDate servicedDate;

    /**
     * End date of the serviced period. Defaults to servicedDate (or today) if null.
     */
    LocalDate servicedPeriodEnd;

    /**
     * One or more NPHIES eligibility purposes.
     * Allowed: {@code benefits}, {@code discovery}, {@code validation}, {@code auth-requirements}.
     */
    @Builder.Default
    List<String> purposes = List.of("benefits");
}
