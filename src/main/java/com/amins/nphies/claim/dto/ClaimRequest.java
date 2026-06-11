package com.amins.nphies.claim.dto;

import com.amins.nphies.claim.Claim;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ClaimRequest {

    private Claim.UseType useType = Claim.UseType.CLAIM;
    private Claim.ClaimType claimType = Claim.ClaimType.INSTITUTIONAL;

    @Size(max = 20)
    private String priority = "normal";

    @NotNull
    private Long beneficiaryId;

    @NotNull
    private Long coverageId;

    private Long encounterId;

    @NotNull
    private Long insurerOrgId;

    @NotNull
    private LocalDate billablePeriodStart;

    @NotNull
    private LocalDate billablePeriodEnd;

    private BigDecimal totalNet;
    private BigDecimal totalGross;
    private String currency = "SAR";

    @Valid
    private List<CareTeamEntry> careTeam;

    @Valid
    private List<DiagnosisEntry> diagnoses;

    @Valid
    private List<ClaimItemDto> items;

    @Getter
    @Setter
    public static class CareTeamEntry {
        private int sequence;
        @NotNull
        private Long practitionerId;
        private String roleCode = "primary";
        private String qualification;
    }

    @Getter
    @Setter
    public static class DiagnosisEntry {
        private int sequence;
        @NotNull
        @Size(max = 20)
        private String icd10Code;
        @Size(max = 255)
        private String icd10Display;
        private String diagnosisType = "principal";
        private String onAdmission;
    }

    @Getter
    @Setter
    public static class ClaimItemDto {
        private int sequence;
        private Integer[] careTeamSequences;
        private Integer[] diagnosisSequences;
        @NotNull
        @Size(max = 50)
        private String productServiceCode;
        @Size(max = 255)
        private String productServiceSystem;
        @NotNull
        private LocalDate servicedDate;
        private BigDecimal quantity = BigDecimal.ONE;
        @NotNull
        private BigDecimal unitPrice;
        @NotNull
        private BigDecimal netAmount;
        @Size(max = 50)
        private String bodySiteCode;
        private String[] modifierCodes;
    }
}
