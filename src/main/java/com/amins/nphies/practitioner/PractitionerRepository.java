package com.amins.nphies.practitioner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PractitionerRepository extends JpaRepository<Practitioner, Long> {

    List<Practitioner> findAllByTenantIdAndActiveTrue(String tenantId);

    Optional<Practitioner> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);

    Optional<Practitioner> findByTenantIdAndPractitionerLicense(String tenantId, String practitionerLicense);

    boolean existsByTenantIdAndPractitionerLicenseAndIdNot(String tenantId, String practitionerLicense, Long id);
}
