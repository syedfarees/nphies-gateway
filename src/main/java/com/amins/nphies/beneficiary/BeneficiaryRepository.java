package com.amins.nphies.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findAllByTenantIdAndActiveTrue(String tenantId);

    Optional<Beneficiary> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);

    Optional<Beneficiary> findByTenantIdAndNationalId(String tenantId, String nationalId);

    boolean existsByTenantIdAndNationalIdAndIdNot(String tenantId, String nationalId, Long id);
}
