package com.amins.nphies.coverage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {

    List<Coverage> findAllByTenantIdAndActiveTrue(String tenantId);

    Optional<Coverage> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);

    List<Coverage> findAllByBeneficiaryIdAndTenantIdAndActiveTrue(Long beneficiaryId, String tenantId);
}
