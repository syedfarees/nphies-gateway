package com.amins.nphies.tenant;

public record TenantRegistrationResponse(
        String tenantId,
        boolean tracareLinked,
        String message
) {}
