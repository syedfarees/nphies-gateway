package com.amins.nphies.beneficiary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TracarePatientDto(
        UUID patientId,
        long iqamaId,
        String firstName,
        String lastName,
        String middleName,
        String gender,
        LocalDate patientDob,
        String email,
        String mobileNumber,
        Long patientMrnNumber
) {}
