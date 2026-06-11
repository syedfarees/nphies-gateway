package com.amins.nphies.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimId(String claimId);

    Optional<Claim> findByClaimIdAndTenantId(String claimId, String tenantId);

    List<Claim> findAllByTenantId(String tenantId);

    List<Claim> findAllByTenantIdAndSubmissionStatus(String tenantId, Claim.SubmissionStatus submissionStatus);
}
