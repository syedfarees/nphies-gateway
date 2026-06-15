package com.amins.nphies.practitioner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PractitionerRepository extends JpaRepository<Practitioner, Long> {

    List<Practitioner> findAllByActiveTrue();

    Optional<Practitioner> findByIdAndActiveTrue(Long id);

    Optional<Practitioner> findByPractitionerLicense(String practitionerLicense);

    boolean existsByPractitionerLicenseAndIdNot(String practitionerLicense, Long id);
}
