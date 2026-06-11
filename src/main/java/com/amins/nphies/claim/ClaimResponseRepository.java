package com.amins.nphies.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimResponseRepository extends JpaRepository<ClaimResponseEntity, Long> {

    List<ClaimResponseEntity> findByClaimId(Long claimId);

    Optional<ClaimResponseEntity> findTopByClaimIdOrderByReceivedAtDesc(Long claimId);
}
