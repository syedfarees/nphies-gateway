package com.amins.nphies.claim;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "claim_diagnoses")
@Getter
@Setter
@NoArgsConstructor
public class ClaimDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "icd10_code", nullable = false, length = 20)
    private String icd10Code;

    @Column(name = "icd10_display", length = 255)
    private String icd10Display;

    @Column(name = "diagnosis_type", nullable = false, length = 50)
    private String diagnosisType = "principal";

    @Column(name = "on_admission", length = 10)
    private String onAdmission;
}
