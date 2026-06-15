package com.amins.nphies.practitioner.dto;

import com.amins.nphies.practitioner.Practitioner;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class PractitionerResponse {

    private final Long id;
    private final String practitionerLicense;
    private final String firstName;
    private final String familyName;
    private final String specialtyCode;
    private final Practitioner.Role role;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public PractitionerResponse(Practitioner p) {
        this.id = p.getId();
        this.practitionerLicense = p.getPractitionerLicense();
        this.firstName = p.getFirstName();
        this.familyName = p.getFamilyName();
        this.specialtyCode = p.getSpecialtyCode();
        this.role = p.getRole();
        this.createdAt = p.getCreatedAt();
        this.updatedAt = p.getUpdatedAt();
    }
}
