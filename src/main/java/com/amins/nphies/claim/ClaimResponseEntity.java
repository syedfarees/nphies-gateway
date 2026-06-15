package com.amins.nphies.claim;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "claim_responses")
@Getter
@Setter
@NoArgsConstructor
public class ClaimResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "nphies_response_id", length = 255)
    private String nphiesResponseId;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "disposition", columnDefinition = "TEXT")
    private String disposition;

    @Column(name = "total_benefit", precision = 12, scale = 2)
    private BigDecimal totalBenefit;

    @Column(name = "total_submitted", precision = 12, scale = 2)
    private BigDecimal totalSubmitted;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "SAR";

    @Column(name = "payment_amount", precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "raw_response_json", columnDefinition = "TEXT")
    private String rawResponseJson;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
