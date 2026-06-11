package com.amins.nphies.tenant;

import com.amins.nphies.config.TenantNphiesConfig;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TenantRegistrationRequest(
        @NotBlank String clinicName,

        // Initial ADMIN user for the new tenant
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotBlank String adminPassword,

        // NPHIES credentials (can be placeholder values updated later)
        @NotBlank String nphiesClientId,
        @NotBlank String nphiesClientSecret,
        @NotBlank String providerLicenseNo,
        @NotBlank String tokenEndpoint,
        @NotBlank String apiBaseUrl,
        @NotNull  TenantNphiesConfig.NphiesEnvironment environment
) {}
