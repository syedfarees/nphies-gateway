package com.amins.nphies.encounter.dto;

import com.amins.nphies.encounter.Encounter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class EncounterRequest {

    @NotNull
    private Long beneficiaryId;

    private Long practitionerId;

    private Encounter.EncounterClass encounterClass = Encounter.EncounterClass.AMB;

    @Size(max = 50)
    private String serviceType;

    @Size(max = 20)
    private String priority = "normal";

    @NotNull
    private OffsetDateTime periodStart;

    private OffsetDateTime periodEnd;

    @Size(max = 50)
    private String admissionSource;

    @Size(max = 50)
    private String dischargeDisposition;

    private Long serviceProviderId;

    @Size(max = 30)
    private String status = "finished";
}
