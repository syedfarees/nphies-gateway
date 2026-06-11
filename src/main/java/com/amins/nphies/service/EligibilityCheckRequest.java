package com.amins.nphies.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class EligibilityCheckRequest {
    @NonNull String    requestId;
    @NonNull String    patientNationalId;
    @NonNull String    patientFirstName;
    @NonNull String    patientFamilyName;
    @NonNull LocalDate patientDateOfBirth;
    @NonNull String    patientGender;       // male|female|other|unknown
    @NonNull String    memberId;
    @Builder.Default String coverageRelationship = "self";
    @NonNull String    payerLicenseNumber;
    @NonNull String    payerName;
    LocalDate          servicedDate;        // null → today
    @Builder.Default List<String> purposes = List.of("benefits");
}
