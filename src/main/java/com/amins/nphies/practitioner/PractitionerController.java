package com.amins.nphies.practitioner;

import com.amins.nphies.practitioner.dto.PractitionerRequest;
import com.amins.nphies.practitioner.dto.PractitionerResponse;
import com.amins.nphies.model.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practitioners")
@RequiredArgsConstructor
public class PractitionerController {

    private final PractitionerService service;

    @GetMapping
    public List<PractitionerResponse> listAll() {
        return service.listAll(TenantContext.require());
    }

    @GetMapping("/{id}")
    public PractitionerResponse getById(
            @PathVariable Long id) {
        return service.getById(TenantContext.require(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PractitionerResponse create(
            @Valid @RequestBody PractitionerRequest request) {
        return service.create(TenantContext.require(), request);
    }

    @PutMapping("/{id}")
    public PractitionerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PractitionerRequest request) {
        return service.update(TenantContext.require(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id) {
        service.delete(TenantContext.require(), id);
    }
}
