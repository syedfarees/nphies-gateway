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
    public ClaimSummaryResponse submitClaim(@Valid @RequestBody ClaimRequest request) {
        TenantContext.require();
        return service.submitClaim(request);
    }

    @GetMapping
    public List<ClaimSummaryResponse> listClaims(
            @RequestParam(required = false) Claim.SubmissionStatus status) {
        TenantContext.require();
        return service.listClaims(status);
    }

    @GetMapping("/{claimId}")
    public ClaimDetailResponse getClaimDetail(@PathVariable String claimId) {
        TenantContext.require();
        return service.getClaimDetail(claimId);
    }

    @PostMapping("/{claimId}/poll")
    public ClaimResponseDto pollClaimResponse(@PathVariable String claimId) {
        TenantContext.require();
        return service.pollClaimResponse(claimId);
    }
}
