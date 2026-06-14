package com.amins.nphies.fhir;

/**
 * NPHIES FHIR Implementation Guide — profile URLs, identifier systems, and
 * terminology code systems.
 *
 * All constants are sourced from the official NPHIES FHIR IG at:
 *   http://nphies.sa/fhir/ksa/nphies-fs/
 */
public final class NphiesProfiles {

    // ── IG version ───────────────────────────────────────────────────────────
    public static final String RESOURCE_VERSION = "1.0.0";

    /** Appends the required IG version suffix to a profile URL. */
    public static String versioned(String profile) {
        return profile + "|" + RESOURCE_VERSION;
    }

    // ── StructureDefinition base ─────────────────────────────────────────────
    private static final String SD = "http://nphies.sa/fhir/ksa/nphies-fs/StructureDefinition/";

    public static final String BUNDLE                = SD + "bundle";
    public static final String MESSAGE_HEADER        = SD + "message-header";
    public static final String ELIGIBILITY_REQUEST   = SD + "eligibility-request";
    public static final String ELIGIBILITY_RESPONSE  = SD + "eligibility-response";
    public static final String PATIENT               = SD + "patient";
    public static final String COVERAGE              = SD + "coverage";
    public static final String PROVIDER_ORGANIZATION = SD + "provider-organization";
    public static final String INSURER_ORGANIZATION  = SD + "insurer-organization";
    public static final String CLAIM                 = SD + "claim";
    public static final String INSTITUTIONAL_CLAIM   = SD + "institutional-claim";
    public static final String CLAIM_RESPONSE        = SD + "claim-response";
    public static final String PRACTITIONER          = SD + "practitioner";
    public static final String PRACTITIONER_ROLE     = SD + "practitioner-role";
    public static final String ENCOUNTER             = SD + "encounter";

    // ── StructureDefinition extensions ───────────────────────────────────────
    public static final String EXT_KSA_ADMIN_GENDER =
            SD + "extension-ksa-administrative-gender";
    public static final String EXT_IDENTIFIER_COUNTRY =
            SD + "extension-identifier-country";
    public static final String EXT_SITE_ELIGIBILITY = SD + "extension-siteEligibility";
    public static final String EXT_TAX              = SD + "extension-tax";
    public static final String EXT_PATIENT_SHARE    = SD + "extension-patient-share";
    public static final String EXT_PAYER_SHARE      = SD + "extension-payer-share";
    public static final String EXT_PACKAGE          = SD + "extension-package";
    public static final String EXT_EPISODE          = SD + "extension-episode";
    public static final String EXT_PATIENT_INVOICE  = SD + "extension-patientInvoice";

    // ── Identifier systems — license numbers ─────────────────────────────────
    /** CCHI-issued provider license number */
    public static final String SYSTEM_PROVIDER_LICENSE =
            "http://nphies.sa/license/provider-license";
    /** CCHI-issued payer/insurer license number */
    public static final String SYSTEM_PAYER_LICENSE =
            "http://nphies.sa/license/payer-license";

    // ── Identifier systems — patient IDs ─────────────────────────────────────
    /** Saudi national ID (10-digit, starts with 1) */
    public static final String SYSTEM_NATIONAL_ID = "http://nphies.sa/identifier/national-id";
    /** Iqama / residency permit (10-digit, starts with 2) */
    public static final String SYSTEM_IQAMA       = "http://nphies.sa/identifier/iqama";

    // ── Identifier systems — coverage ────────────────────────────────────────
    /** Insurance member / beneficiary ID — payer-specific; override via EligibilityRequestInput */
    public static final String SYSTEM_MEMBER_ID = "http://payer.com/memberid";

    // ── Identifier systems — clinical ────────────────────────────────────────
    /** CCHI-issued practitioner license number (used as Practitioner.identifier) */
    public static final String SYSTEM_PRACTITIONER_LICENSE =
            "http://nphies.sa/license/practitioner-license";
    /** Practitioner identifier system used inside PractitionerRole.practitioner.identifier */
    public static final String SYSTEM_PRACTITIONER_LICENSES =
            "http://nphies.sa/licenses/practitioner";
    public static final String SYSTEM_ENCOUNTER = "http://nphies.sa/identifier/encounter";
    public static final String SYSTEM_CLAIM     = "http://nphies.sa/identifier/claim";

    // ── Terminology code systems — NPHIES ────────────────────────────────────
    private static final String CS = "http://nphies.sa/terminology/CodeSystem/";

    /** NPHIES message event codes (eligibility-request, claim-request, …) */
    public static final String CS_MESSAGE_EVENTS   = CS + "ksa-message-events";
    /** Coverage type codes (EHCPOL, PUBLICPOL, …) */
    public static final String CS_COVERAGE_TYPE    = CS + "coverage-type";
    /** Organization type codes (prov, ins, …) */
    public static final String CS_ORG_TYPE         = CS + "organization-type";
    /** KSA administrative gender extension codes */
    public static final String CS_KSA_ADMIN_GENDER = CS + "ksa-administrative-gender";
    public static final String CS_DIAGNOSIS_TYPE   = CS + "diagnosis-type";
    public static final String CS_DIAGNOSIS_ON_ADMISSION = CS + "diagnosis-on-admission";
    public static final String CS_SERVICE_TYPE     = CS + "service-type";
    public static final String CS_PROCEDURE        = CS + "procedure";
    public static final String CS_CLAIM_SUBTYPE    = CS + "claim-subtype";
    /** PractitionerRole code system for role codes (doctor, nurse, …) */
    public static final String CS_PRACTITIONER_ROLE = CS + "practitioner-role";
    /** Practice specialty codes (e.g. 08.22 = Hematology) */
    public static final String CS_PRACTICE_CODES   = CS + "practice-codes";
    /** Claim supporting information category codes */
    public static final String CS_CLAIM_INFO_CATEGORY = CS + "claim-information-category";
    /** Site eligibility response codes (eligible, not-eligible, …) */
    public static final String CS_SITE_ELIGIBILITY   = CS + "siteEligibility";

    // ── Terminology code systems — standard HL7 ──────────────────────────────
    public static final String CS_ELIGIBILITY_PURPOSE =
            "http://hl7.org/fhir/eligibilityrequest-purpose";
    public static final String CS_RELATIONSHIP =
            "http://terminology.hl7.org/CodeSystem/subscriber-relationship";
    /** Process priority codes (stat, normal, deferred) */
    public static final String CS_PROCESS_PRIORITY =
            "http://terminology.hl7.org/CodeSystem/processpriority";
    public static final String CS_CLAIM_TYPE =
            "http://terminology.hl7.org/CodeSystem/claim-type";
    public static final String CS_CLAIM_USE =
            "http://hl7.org/fhir/claim-use";
    public static final String CS_CARE_TEAM_ROLE =
            "http://terminology.hl7.org/CodeSystem/claimcareteamrole";
    public static final String CS_ENCOUNTER_CLASS =
            "http://terminology.hl7.org/CodeSystem/v3-ActCode";
    /** HL7 v2 identifier type codes (NI = national ID, PRC = permanent resident card) */
    public static final String CS_V2_0203 =
            "http://terminology.hl7.org/CodeSystem/v2-0203";
    /** HL7 v3 marital status codes (M, S, D, W, …) */
    public static final String CS_V3_MARITAL_STATUS =
            "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus";
    public static final String CS_ICD10 = "http://hl7.org/fhir/sid/icd-10-am";
    /** Payee type codes (provider, subscriber, other) */
    public static final String CS_PAYEE_TYPE = "http://terminology.hl7.org/CodeSystem/payeetype";
    /** UCUM unit system for quantities */
    public static final String CS_UCUM       = "http://unitsofmeasure.org";

    private NphiesProfiles() {}
}
