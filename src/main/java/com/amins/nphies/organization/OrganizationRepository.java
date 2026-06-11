package com.amins.nphies.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findAllByTenantIdAndActiveTrue(String tenantId);

    Optional<Organization> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);

    Optional<Organization> findByTenantIdAndLicenseNo(String tenantId, String licenseNo);

    boolean existsByTenantIdAndLicenseNoAndIdNot(String tenantId, String licenseNo, Long id);
}
