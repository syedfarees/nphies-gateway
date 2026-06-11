package com.amins.nphies.claim;

import com.amins.nphies.claim.dto.ClaimDetailResponse;
import com.amins.nphies.claim.dto.ClaimRequest;
import com.amins.nphies.claim.dto.ClaimResponseDto;
import com.amins.nphies.claim.dto.ClaimSummaryResponse;
import com.amins.nphies.model.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimSummaryResponse submitClaim(
            @Valid @RequestBody ClaimRequest request) {
        return service.submitClaim(TenantContext.require(), request);
    }

    @GetMapping
    public List<ClaimSummaryResponse> listClaims(
            @RequestParam(required = false) Claim.SubmissionStatus status) {
        return service.listClaims(TenantContext.require(), status);
    }

    @GetMapping("/{claimId}")
    public ClaimDetailResponse getClaimDetail(
            @PathVariable String claimId) {
        return service.getClaimDetail(TenantContext.require(), claimId);
    }

    @PostMapping("/{claimId}/poll")
    public ClaimResponseDto pollClaimResponse(
            @PathVariable String claimId) {
        return service.pollClaimResponse(TenantContext.require(), claimId);
    }
}
