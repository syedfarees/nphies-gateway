package com.amins.nphies.claim.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class ClaimResponseDto {

    private final String bundleId;
    private final String outcome;
    private final String disposition;
    private final String adjudicationOutcomeCode;
    private final BigDecimal totalBenefit;
    private final BigDecimal totalSubmitted;
    private final BigDecimal paymentAmount;
    private final LocalDate paymentDate;
    private final String currency;
    private final OffsetDateTime receivedAt;
}
