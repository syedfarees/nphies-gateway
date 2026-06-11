package com.amins.nphies.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimCareTeamRepository extends JpaRepository<ClaimCareTeam, Long> {

    List<ClaimCareTeam> findAllByClaimIdOrderBySequence(Long claimId);
}
