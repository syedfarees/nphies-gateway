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
        TenantContext.require();
        return service.listAll();
    }

    @GetMapping("/{id}")
    public OrganizationResponse getById(@PathVariable Long id) {
        TenantContext.require();
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody OrganizationRequest request) {
        TenantContext.require();
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable Long id,
                                        @Valid @RequestBody OrganizationRequest request) {
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
