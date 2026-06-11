package com.amins.nphies.coverage.dto;

import com.amins.nphies.coverage.Coverage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CoverageRequest {

    @NotNull
    private Long beneficiaryId;

    @NotBlank
    @Size(max = 100)
    private String memberId;

    @Size(max = 100)
    private String subscriberId;

    @NotBlank
    @Size(max = 100)
    private String payerLicenseNo;

    @NotBlank
    @Size(max = 255)
    private String payerName;

    @Size(max = 50)
    private String coverageRelationship = "self";

    private LocalDate periodStart;
    private LocalDate periodEnd;

    @Size(max = 100)
    private String classValue;

    @Size(max = 100)
    private String className;

    private int orderOfBenefit = 1;

    private Coverage.Status status = Coverage.Status.ACTIVE;
}
