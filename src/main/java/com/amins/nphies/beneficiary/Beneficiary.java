package com.amins.nphies.beneficiary;

import com.fasterxml.jackson.annotation.JsonCreator;
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
@Table(name = "beneficiaries")
@Getter
@Setter
@NoArgsConstructor
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "national_id", nullable = false, length = 50)
    private String nationalId;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id_type", nullable = false, length = 20)
    private IdType idType = IdType.NATIONAL_ID;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "family_name", nullable = false, length = 100)
    private String familyName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "member_id", length = 100)
    private String memberId;

    @Column(name = "payer_license_no", length = 100)
    private String payerLicenseNo;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "coverage_relationship", nullable = false, length = 50)
    private String coverageRelationship = "self";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum IdType {
        NATIONAL_ID, IQAMA;

        @JsonCreator
        public static IdType fromValue(String value) {
            if (value == null || value.isBlank()) return null;
            return IdType.valueOf(value.trim().toUpperCase());
        }
    }
}
