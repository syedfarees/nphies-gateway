package com.amins.nphies.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findAllByActiveTrue();

    Optional<Beneficiary> findByIdAndActiveTrue(Long id);

    Optional<Beneficiary> findByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);
}
