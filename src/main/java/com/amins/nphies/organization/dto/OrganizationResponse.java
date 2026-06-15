package com.amins.nphies.organization.dto;

import com.amins.nphies.organization.Organization;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class OrganizationResponse {

    private final Long id;
    private final String licenseNo;
    private final Organization.OrgType orgType;
    private final String name;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public OrganizationResponse(Organization o) {
        this.id = o.getId();
        this.licenseNo = o.getLicenseNo();
        this.orgType = o.getOrgType();
        this.name = o.getName();
        this.createdAt = o.getCreatedAt();
        this.updatedAt = o.getUpdatedAt();
    }
}
