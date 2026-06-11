package com.amins.nphies.claim;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "claim_id", nullable = false, unique = true, length = 100)
    private String claimId;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "use_type", nullable = false, length = 30)
    private UseType useType = UseType.CLAIM;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "claim_type", nullable = false, length = 30)
    private ClaimType claimType = ClaimType.INSTITUTIONAL;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority = "normal";

    @Column(name = "beneficiary_id", nullable = false)
    private Long beneficiaryId;

    @Column(name = "coverage_id", nullable = false)
    private Long coverageId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "insurer_org_id", nullable = false)
    private Long insurerOrgId;

    @Column(name = "billable_period_start", nullable = false)
    private LocalDate billablePeriodStart;

    @Column(name = "billable_period_end", nullable = false)
    private LocalDate billablePeriodEnd;

    @Column(name = "total_net", precision = 12, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "total_gross", precision = 12, scale = 2)
    private BigDecimal totalGross;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "SAR";

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "submission_status", nullable = false, length = 20)
    private SubmissionStatus submissionStatus = SubmissionStatus.PENDING;

    @Column(name = "nphies_bundle_id", length = 255)
    private String nphiesBundleId;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum UseType {
        CLAIM, PREAUTHORIZATION, PREDETERMINATION;

        public String toFhirCode() {
            return name().toLowerCase();
        }
    }

    public enum ClaimType {
        INSTITUTIONAL, PROFESSIONAL, ORAL, PHARMACY, VISION;

        public String toFhirCode() {
            return name().toLowerCase();
        }
    }

    public enum SubmissionStatus {
        PENDING, SUBMITTED, ERROR
    }
}
