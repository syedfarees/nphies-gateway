package com.amins.nphies.beneficiary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TracareTenantDto(String tenantClientId, String dbName, String status) {}
