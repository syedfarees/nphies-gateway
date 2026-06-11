package com.amins.nphies.encounter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    List<Encounter> findAllByTenantIdAndActiveTrue(String tenantId);

    Optional<Encounter> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);

    List<Encounter> findAllByBeneficiaryIdAndTenantIdAndActiveTrue(Long beneficiaryId, String tenantId);
}
