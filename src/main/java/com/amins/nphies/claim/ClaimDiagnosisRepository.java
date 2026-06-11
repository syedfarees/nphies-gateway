package com.amins.nphies.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimDiagnosisRepository extends JpaRepository<ClaimDiagnosis, Long> {

    List<ClaimDiagnosis> findAllByClaimIdOrderBySequence(Long claimId);
}
