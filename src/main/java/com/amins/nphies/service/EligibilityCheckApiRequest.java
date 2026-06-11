package com.amins.nphies.service;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EligibilityCheckApiRequest {

    @NotNull
    private Long beneficiaryId;

    /** Insurance membership ID — overrides what is stored on the beneficiary. */
    private String memberId;

    private String payerLicenseNo;
    private String payerName;
    private LocalDate servicedDate;
}
