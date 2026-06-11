package com.amins.nphies.fhir;

/**
 * NPHIES FHIR Implementation Guide — profile URLs, identifier systems, and
 * terminology code systems.
 *
 * All constants are sourced from the official NPHIES FHIR IG at:
 *   http://nphies.sa/fhir/ksa/nphies-fs/
 */
public final class NphiesProfiles {

    // ── StructureDefinition base ─────────────────────────────────────────────
    private static final String SD = "http://nphies.sa/fhir/ksa/nphies-fs/StructureDefinition/";

    public static final String BUNDLE                     = SD + "bundle";
    public static final String MESSAGE_HEADER             = SD + "message-header";
    public static final String ELIGIBILITY_REQUEST        = SD + "eligibility-request";
    public static final String PATIENT                    = SD + "patient";
    public static final String COVERAGE                   = SD + "coverage";
    public static final String PROVIDER_ORGANIZATION      = SD + "provider-organization";
    public static final String INSURER_ORGANIZATION       = SD + "insurer-organization";

    // ── Identifier systems ───────────────────────────────────────────────────
    private static final String ID = "http://nphies.sa/identifier/ksa/";

    /** Saudi national ID (10-digit, starts with 1) */
    public static final String SYSTEM_NATIONAL_ID      = ID + "national-id";
    /** Iqama / residency permit (10-digit, starts with 2) */
    public static final String SYSTEM_IQAMA            = ID + "iqama";
    /** CCHI-issued provider license number */
    public static final String SYSTEM_PROVIDER_LICENSE = ID + "provider-license";
    /** CCHI-issued payer/insurer license number */
    public static final String SYSTEM_PAYER_LICENSE    = ID + "payer-license";
    /** Insurance member / beneficiary ID issued by the payer */
    public static final String SYSTEM_MEMBER_ID        = ID + "member-id";

    // ── Terminology code systems ─────────────────────────────────────────────
    private static final String CS = "http://nphies.sa/terminology/CodeSystem/";

    /** NPHIES message event codes (eligibility-request, claim-request, …) */
    public static final String CS_MESSAGE_EVENTS = CS + "ksa-message-events";

    // ── Standard HL7 FHIR systems used by NPHIES ────────────────────────────
    public static final String CS_ELIGIBILITY_PURPOSE =
            "http://hl7.org/fhir/eligibilityrequest-purpose";

    public static final String CS_RELATIONSHIP =
            "http://terminology.hl7.org/CodeSystem/subscriber-relationship";

    // ── Additional StructureDefinition profiles ───────────────────────────────
    public static final String CLAIM               = SD + "claim";
    public static final String CLAIM_RESPONSE      = SD + "claim-response";
    public static final String PRACTITIONER        = SD + "practitioner";
    public static final String ENCOUNTER           = SD + "encounter";

    // ── Additional identifier systems ─────────────────────────────────────────
    public static final String SYSTEM_PRACTITIONER_LICENSE = ID + "practitioner-license";
    public static final String SYSTEM_ENCOUNTER            = ID + "encounter";
    public static final String SYSTEM_CLAIM                = ID + "claim";

    // ── Additional code systems ───────────────────────────────────────────────
    public static final String CS_CLAIM_TYPE      = "http://terminology.hl7.org/CodeSystem/claim-type";
    public static final String CS_CLAIM_USE       = "http://hl7.org/fhir/claim-use";
    public static final String CS_CARE_TEAM_ROLE  = "http://terminology.hl7.org/CodeSystem/claimcareteamrole";
    public static final String CS_DIAGNOSIS_TYPE  = CS + "diagnosis-type";
    public static final String CS_ENCOUNTER_CLASS = "http://terminology.hl7.org/CodeSystem/v3-ActCode";
    public static final String CS_SERVICE_TYPE    = CS + "service-type";
    public static final String CS_ICD10           = "http://hl7.org/fhir/sid/icd-10-am";
    public static final String CS_PROCEDURE       = CS + "procedure";
    public static final String CS_PRIORITY        = "http://terminology.hl7.org/CodeSystem/processpriority";

    private NphiesProfiles() {}
}
