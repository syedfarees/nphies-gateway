package com.amins.nphies.encounter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    List<Encounter> findAllByActiveTrue();

    Optional<Encounter> findByIdAndActiveTrue(Long id);

    List<Encounter> findAllByBeneficiaryIdAndActiveTrue(Long beneficiaryId);
}
