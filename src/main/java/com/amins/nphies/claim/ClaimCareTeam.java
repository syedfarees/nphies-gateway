package com.amins.nphies.claim;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "claim_care_team")
@Getter
@Setter
@NoArgsConstructor
public class ClaimCareTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "practitioner_id", nullable = false)
    private Long practitionerId;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode = "primary";

    @Column(name = "qualification", length = 100)
    private String qualification;
}
