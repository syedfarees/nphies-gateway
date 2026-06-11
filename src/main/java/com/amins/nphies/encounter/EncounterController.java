package com.amins.nphies.encounter;

import com.amins.nphies.encounter.dto.EncounterRequest;
import com.amins.nphies.encounter.dto.EncounterResponse;
import com.amins.nphies.model.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService service;

    @GetMapping
    public List<EncounterResponse> listAll() {
        return service.listAll(TenantContext.require());
    }

    @GetMapping("/{id}")
    public EncounterResponse getById(
            @PathVariable Long id) {
        return service.getById(TenantContext.require(), id);
    }

    @GetMapping("/beneficiary/{beneficiaryId}")
    public List<EncounterResponse> listByBeneficiary(
            @PathVariable Long beneficiaryId) {
        return service.listByBeneficiary(TenantContext.require(), beneficiaryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EncounterResponse create(
            @Valid @RequestBody EncounterRequest request) {
        return service.create(TenantContext.require(), request);
    }

    @PutMapping("/{id}")
    public EncounterResponse update(
            @PathVariable Long id,
            @Valid @RequestBody EncounterRequest request) {
        return service.update(TenantContext.require(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id) {
        service.delete(TenantContext.require(), id);
    }
}
