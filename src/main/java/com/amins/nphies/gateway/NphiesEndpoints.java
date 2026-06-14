package com.amins.nphies.gateway;

import com.amins.nphies.config.TenantNphiesConfig.NphiesEnvironment;

/**
 * NPHIES API endpoint constants per environment.
 * Override per-tenant using apiBaseUrl on TenantNphiesConfig
 * if a tenant has a non-standard URL.
 */
public final class NphiesEndpoints {

    // ── Base URLs ────────────────────────────────────────────────────────────
    public static final String UAT_BASE_URL  = "https://HSB.nphies.sa/r4";
    public static final String PROD_BASE_URL = "https://api.nphies.sa/r4";

    public static final String UAT_TOKEN_URL  = "https://HSB.nphies.sa/auth/realms/nphies/protocol/openid-connect/token";
    public static final String PROD_TOKEN_URL = "https://api.nphies.sa/auth/realms/nphies/protocol/openid-connect/token";

    // ── FHIR Endpoints ───────────────────────────────────────────────────────
    public static final String BUNDLE_ENDPOINT          = "/Bundle";
    public static final String CLAIM_RESPONSE_ENDPOINT  = "/ClaimResponse";
    public static final String COMMUNICATION_ENDPOINT   = "/Communication";

    // ── MessageHeader destination endpoint ───────────────────────────────────
    /** Fixed NPHIES routing/connectivity server endpoint used in MessageHeader.destination */
    public static final String NPHIES_DESTINATION_ENDPOINT = "http://10.1.24.10/";

    // ── FHIR MessageHeader event codes (NPHIES-specific) ────────────────────
    public static final String EVENT_ELIGIBILITY_REQUEST  = "eligibility-request";
    public static final String EVENT_PRIORAUTH_REQUEST    = "priorauth-request";
    public static final String EVENT_CLAIM_REQUEST        = "claim-request";
    public static final String EVENT_COMMUNICATION        = "communication";

    public static String baseUrlFor(NphiesEnvironment env) {
        return env == NphiesEnvironment.PRODUCTION ? PROD_BASE_URL : UAT_BASE_URL;
    }

    public static String tokenUrlFor(NphiesEnvironment env) {
        return env == NphiesEnvironment.PRODUCTION ? PROD_TOKEN_URL : UAT_TOKEN_URL;
    }

    private NphiesEndpoints() {}
}
