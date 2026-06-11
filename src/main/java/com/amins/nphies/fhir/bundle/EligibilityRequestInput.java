package com.amins.nphies.fhir.bundle;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * Caller-supplied data for building a NPHIES CoverageEligibilityRequest Bundle.
 *
 * Typical usage:
 * <pre>
 *   EligibilityRequestInput input = EligibilityRequestInput.builder()
 *       .requestId(UUID.randomUUID().toString())
 *       .patientNationalId("1234567890")
 *       ...
 *       .build();
 *   String bundleJson = builder.build(input);
 * </pre>
 */
@Value
@Builder
public class EligibilityRequestInput {

    // ── Request identity ─────────────────────────────────────────────────────

    /** UUID for the Bundle.id and transaction tracking. */
    @NonNull String requestId;

    // ── Patient ──────────────────────────────────────────────────────────────

    /**
     * Saudi national ID (starts with 1) or Iqama number (starts with 2).
     * The builder selects the correct NPHIES identifier system automatically.
     */
    @NonNull String patientNationalId;

    @NonNull String patientFirstName;
    @NonNull String patientFamilyName;

    @NonNull LocalDate patientDateOfBirth;

    /**
     * FHIR administrative gender: {@code male}, {@code female}, {@code other}, {@code unknown}.
     */
    @NonNull String patientGender;

    // ── Coverage / insurance ─────────────────────────────────────────────────

    /** Member/beneficiary ID issued by the insurer. */
    @NonNull String memberId;

    /**
     * Subscriber relationship code per
     * {@code http://terminology.hl7.org/CodeSystem/subscriber-relationship}.
     * Typical values: {@code self}, {@code child}, {@code spouse}.
     */
    @Builder.Default
    String coverageRelationship = "self";

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
     * Date for which eligibility is being checked. Defaults to today if null.
     */
    LocalDate servicedDate;

    /**
     * One or more NPHIES eligibility purposes.
     * Allowed values: {@code benefits}, {@code discovery}, {@code validation},
     * {@code auth-requirements}.
     * Defaults to {@code ["benefits"]} if null/empty.
     */
    @Builder.Default
    List<String> purposes = List.of("benefits");
}
