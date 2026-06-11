package com.amins.nphies.claim.dto;

import com.amins.nphies.claim.Claim;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
public class ClaimSummaryResponse {

    private final String claimId;
    private final Claim.UseType useType;
    private final Claim.ClaimType claimType;
    private final Claim.SubmissionStatus submissionStatus;
    private final String nphiesBundleId;
    private final OffsetDateTime submittedAt;
    private final BigDecimal totalNet;
    private final String currency;

    public ClaimSummaryResponse(Claim c) {
        this.claimId = c.getClaimId();
        this.useType = c.getUseType();
        this.claimType = c.getClaimType();
        this.submissionStatus = c.getSubmissionStatus();
        this.nphiesBundleId = c.getNphiesBundleId();
        this.submittedAt = c.getSubmittedAt();
        this.totalNet = c.getTotalNet();
        this.currency = c.getCurrency();
    }
}
