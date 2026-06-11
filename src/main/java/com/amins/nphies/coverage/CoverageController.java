package com.amins.nphies.coverage;

import com.amins.nphies.coverage.dto.CoverageRequest;
import com.amins.nphies.coverage.dto.CoverageResponse;
import com.amins.nphies.model.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coverages")
@RequiredArgsConstructor
public class CoverageController {

    private final CoverageService service;

    @GetMapping
    public List<CoverageResponse> listAll() {
        return service.listAll(TenantContext.require());
    }

    @GetMapping("/{id}")
    public CoverageResponse getById(
            @PathVariable Long id) {
        return service.getById(TenantContext.require(), id);
    }

    @GetMapping("/beneficiary/{beneficiaryId}")
    public List<CoverageResponse> listByBeneficiary(
            @PathVariable Long beneficiaryId) {
        return service.listByBeneficiary(TenantContext.require(), beneficiaryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoverageResponse create(
            @Valid @RequestBody CoverageRequest request) {
        return service.create(TenantContext.require(), request);
    }

    @PutMapping("/{id}")
    public CoverageResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CoverageRequest request) {
        return service.update(TenantContext.require(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id) {
        service.delete(TenantContext.require(), id);
    }
}
