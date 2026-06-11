package com.amins.nphies.beneficiary.dto;

import com.amins.nphies.beneficiary.Beneficiary;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BeneficiaryRequest {

    @NotBlank
    @Size(max = 50)
    private String nationalId;

    private Beneficiary.IdType idType = Beneficiary.IdType.NATIONAL_ID;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String familyName;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "male|female|unknown", message = "gender must be male, female, or unknown")
    private String gender;

    // Normalizes any case variant (MALE, Male) to the required lowercase form before validation runs
    public void setGender(String gender) {
        if (gender == null) { this.gender = null; return; }
        String lower = gender.trim().toLowerCase();
        this.gender = lower.equals("male") || lower.equals("female") ? lower : "unknown";
    }

    @Size(max = 100)
    private String memberId;

    @Size(max = 100)
    private String payerLicenseNo;

    @Size(max = 255)
    private String payerName;

    @Size(max = 50)
    private String coverageRelationship = "self";

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;
}
