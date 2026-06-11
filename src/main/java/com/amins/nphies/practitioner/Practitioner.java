package com.amins.nphies.practitioner;

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
@Table(name = "practitioners")
@Getter
@Setter
@NoArgsConstructor
public class Practitioner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "practitioner_license", nullable = false, length = 100)
    private String practitionerLicense;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "family_name", nullable = false, length = 100)
    private String familyName;

    @Column(name = "specialty_code", length = 50)
    private String specialtyCode;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.DOCTOR;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Role {
        DOCTOR, NURSE, PHARMACIST
    }
}
