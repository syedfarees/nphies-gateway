package com.amins.nphies.beneficiary;

import com.amins.nphies.beneficiary.dto.BeneficiaryRequest;
import com.amins.nphies.beneficiary.dto.BeneficiaryResponse;
import com.amins.nphies.beneficiary.dto.TracarePatientDto;
import com.amins.nphies.model.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService service;
    private final TracareAppClient tracareAppClient;

    @GetMapping
    public List<BeneficiaryResponse> listAll() {
        TenantContext.require();
        return service.listAll();
    }

    @GetMapping("/{id}")
    public BeneficiaryResponse getById(@PathVariable Long id) {
        TenantContext.require();
        return service.getById(id);
    }

    @GetMapping("/search")
    public BeneficiaryResponse searchByNationalId(@RequestParam String nationalId) {
        TenantContext.require();
        return service.getByNationalId(nationalId);
    }

    @GetMapping("/tracare-lookup")
    public ResponseEntity<TracarePatientDto> lookupFromTracare(@RequestParam String idNumber) {
        return tracareAppClient.findPatientByIdNumber(idNumber, TenantContext.require())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upsert")
    public BeneficiaryResponse upsert(@Valid @RequestBody BeneficiaryRequest request) {
        TenantContext.require();
        return service.upsert(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse create(@Valid @RequestBody BeneficiaryRequest request) {
        TenantContext.require();
        return service.create(request);
    }

    @PutMapping("/{id}")
    public BeneficiaryResponse update(@PathVariable Long id,
                                       @Valid @RequestBody BeneficiaryRequest request) {
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
