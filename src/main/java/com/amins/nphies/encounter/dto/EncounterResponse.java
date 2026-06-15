package com.amins.nphies.encounter.dto;

import com.amins.nphies.encounter.Encounter;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class EncounterResponse {

    private final Long id;
    private final Long beneficiaryId;
    private final Long practitionerId;
    private final Encounter.EncounterClass encounterClass;
    private final String serviceType;
    private final String priority;
    private final OffsetDateTime periodStart;
    private final OffsetDateTime periodEnd;
    private final String admissionSource;
    private final String dischargeDisposition;
    private final Long serviceProviderId;
    private final String status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public EncounterResponse(Encounter e) {
        this.id = e.getId();
        this.beneficiaryId = e.getBeneficiaryId();
        this.practitionerId = e.getPractitionerId();
        this.encounterClass = e.getEncounterClass();
        this.serviceType = e.getServiceType();
        this.priority = e.getPriority();
        this.periodStart = e.getPeriodStart();
        this.periodEnd = e.getPeriodEnd();
        this.admissionSource = e.getAdmissionSource();
        this.dischargeDisposition = e.getDischargeDisposition();
        this.serviceProviderId = e.getServiceProviderId();
        this.status = e.getStatus();
        this.createdAt = e.getCreatedAt();
        this.updatedAt = e.getUpdatedAt();
    }
}
