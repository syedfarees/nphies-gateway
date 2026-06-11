package com.amins.nphies.coverage;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coverages")
@Getter
@Setter
@NoArgsConstructor
public class Coverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "beneficiary_id", nullable = false)
    private Long beneficiaryId;

    @Column(name = "member_id", nullable = false, length = 100)
    private String memberId;

    @Column(name = "subscriber_id", length = 100)
    private String subscriberId;

    @Column(name = "payer_license_no", nullable = false, length = 100)
    private String payerLicenseNo;

    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;

    @Column(name = "coverage_relationship", nullable = false, length = 50)
    private String coverageRelationship = "self";

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "class_value", length = 100)
    private String classValue;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "order_of_benefit", nullable = false)
    private int orderOfBenefit = 1;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Status {
        ACTIVE, CANCELLED, DRAFT, ENTERED_IN_ERROR;

        public String toFhirCode() {
            return switch (this) {
                case ACTIVE -> "active";
                case CANCELLED -> "cancelled";
                case DRAFT -> "draft";
                case ENTERED_IN_ERROR -> "entered-in-error";
            };
        }
    }
}
