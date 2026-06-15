package com.amins.nphies.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findAllByActiveTrue();

    Optional<Organization> findByIdAndActiveTrue(Long id);

    Optional<Organization> findByLicenseNo(String licenseNo);

    boolean existsByLicenseNoAndIdNot(String licenseNo, Long id);
}
