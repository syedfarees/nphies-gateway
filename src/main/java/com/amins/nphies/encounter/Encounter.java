package com.amins.nphies.encounter;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "encounters")
@Getter
@Setter
@NoArgsConstructor
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beneficiary_id", nullable = false)
    private Long beneficiaryId;

    @Column(name = "practitioner_id")
    private Long practitionerId;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "encounter_class", nullable = false, length = 20)
    private EncounterClass encounterClass = EncounterClass.AMB;

    @Column(name = "service_type", length = 50)
    private String serviceType;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority = "normal";

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end")
    private OffsetDateTime periodEnd;

    @Column(name = "admission_source", length = 50)
    private String admissionSource;

    @Column(name = "discharge_disposition", length = 50)
    private String dischargeDisposition;

    @Column(name = "service_provider_id")
    private Long serviceProviderId;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "finished";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum EncounterClass {
        AMB, EMER, IMP, SS, HH
    }
}
