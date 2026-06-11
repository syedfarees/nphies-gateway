package com.amins.nphies.claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimItemRepository extends JpaRepository<ClaimItem, Long> {

    List<ClaimItem> findAllByClaimIdOrderBySequence(Long claimId);
}
