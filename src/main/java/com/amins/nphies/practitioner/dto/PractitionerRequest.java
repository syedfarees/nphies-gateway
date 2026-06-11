package com.amins.nphies.practitioner.dto;

import com.amins.nphies.practitioner.Practitioner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PractitionerRequest {

    @NotBlank
    @Size(max = 100)
    private String practitionerLicense;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String familyName;

    @Size(max = 50)
    private String specialtyCode;

    private Practitioner.Role role = Practitioner.Role.DOCTOR;
}
