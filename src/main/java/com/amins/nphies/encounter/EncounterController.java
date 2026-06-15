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
        TenantContext.require();
        return service.listAll();
    }

    @GetMapping("/{id}")
    public EncounterResponse getById(@PathVariable Long id) {
        TenantContext.require();
        return service.getById(id);
    }

    @GetMapping("/beneficiary/{beneficiaryId}")
    public List<EncounterResponse> listByBeneficiary(@PathVariable Long beneficiaryId) {
        TenantContext.require();
        return service.listByBeneficiary(beneficiaryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EncounterResponse create(@Valid @RequestBody EncounterRequest request) {
        TenantContext.require();
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EncounterResponse update(@PathVariable Long id,
                                     @Valid @RequestBody EncounterRequest request) {
        TenantContext.require();
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        TenantContext.require();
        service.delete(id);
    }
}
