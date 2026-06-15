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
        TenantContext.require();
        return service.listAll();
    }

    @GetMapping("/{id}")
    public PractitionerResponse getById(@PathVariable Long id) {
        TenantContext.require();
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PractitionerResponse create(@Valid @RequestBody PractitionerRequest request) {
        TenantContext.require();
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PractitionerResponse update(@PathVariable Long id,
                                        @Valid @RequestBody PractitionerRequest request) {
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
