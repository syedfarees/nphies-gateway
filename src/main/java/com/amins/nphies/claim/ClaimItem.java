package com.amins.nphies.claim;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "claim_items")
@Getter
@Setter
@NoArgsConstructor
public class ClaimItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "care_team_sequences", columnDefinition = "json")
    private Integer[] careTeamSequences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagnosis_sequences", columnDefinition = "json")
    private Integer[] diagnosisSequences;

    @Column(name = "product_service_code", nullable = false, length = 50)
    private String productServiceCode;

    @Column(name = "product_service_system", length = 255)
    private String productServiceSystem;

    @Column(name = "serviced_date", nullable = false)
    private LocalDate servicedDate;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "body_site_code", length = 50)
    private String bodySiteCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modifier_codes", columnDefinition = "json")
    private String[] modifierCodes;
}
