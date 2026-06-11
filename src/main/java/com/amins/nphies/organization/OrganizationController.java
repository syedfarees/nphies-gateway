package com.amins.nphies.organization;

import com.amins.nphies.organization.dto.OrganizationRequest;
import com.amins.nphies.organization.dto.OrganizationResponse;
import com.amins.nphies.model.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService service;

    @GetMapping
    public List<OrganizationResponse> listAll() {
        return service.listAll(TenantContext.require());
    }

    @GetMapping("/{id}")
    public OrganizationResponse getById(
            @PathVariable Long id) {
        return service.getById(TenantContext.require(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(
            @Valid @RequestBody OrganizationRequest request) {
        return service.create(TenantContext.require(), request);
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest request) {
        return service.update(TenantContext.require(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id) {
        service.delete(TenantContext.require(), id);
    }
}
