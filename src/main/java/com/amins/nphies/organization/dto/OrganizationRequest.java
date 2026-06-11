package com.amins.nphies.organization.dto;

import com.amins.nphies.organization.Organization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationRequest {

    @NotBlank
    @Size(max = 100)
    private String licenseNo;

    private Organization.OrgType orgType = Organization.OrgType.INSURER;

    @NotBlank
    @Size(max = 255)
    private String name;
}
