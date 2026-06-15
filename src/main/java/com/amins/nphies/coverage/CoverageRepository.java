package com.amins.nphies.coverage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {

    List<Coverage> findAllByActiveTrue();

    Optional<Coverage> findByIdAndActiveTrue(Long id);

    List<Coverage> findAllByBeneficiaryIdAndActiveTrue(Long beneficiaryId);
}
