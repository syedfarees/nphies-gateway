package com.amins.nphies.practitioner;

import com.amins.nphies.practitioner.dto.PractitionerRequest;
import com.amins.nphies.practitioner.dto.PractitionerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PractitionerService {

    private final PractitionerRepository repository;

    @Transactional(readOnly = true)
    public List<PractitionerResponse> listAll(String tenantId) {
        return repository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(PractitionerResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PractitionerResponse getById(String tenantId, Long id) {
        return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .map(PractitionerResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner not found"));
    }

    @Transactional
    public PractitionerResponse create(String tenantId, PractitionerRequest req) {
        repository.findByTenantIdAndPractitionerLicense(tenantId, req.getPractitionerLicense())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Practitioner with license " + req.getPractitionerLicense() + " already exists for this tenant");
                });

        Practitioner p = mapToEntity(new Practitioner(), tenantId, req);
        return new PractitionerResponse(repository.save(p));
    }

    @Transactional
    public PractitionerResponse update(String tenantId, Long id, PractitionerRequest req) {
        Practitioner p = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner not found"));

        if (!p.getPractitionerLicense().equals(req.getPractitionerLicense()) &&
                repository.existsByTenantIdAndPractitionerLicenseAndIdNot(tenantId, req.getPractitionerLicense(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another practitioner with license " + req.getPractitionerLicense() + " already exists");
        }

        return new PractitionerResponse(repository.save(mapToEntity(p, tenantId, req)));
    }

    @Transactional
    public void delete(String tenantId, Long id) {
        Practitioner p = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner not found"));
        p.setActive(false);
        repository.save(p);
    }

    private Practitioner mapToEntity(Practitioner p, String tenantId, PractitionerRequest req) {
        p.setTenantId(tenantId);
        p.setPractitionerLicense(req.getPractitionerLicense());
        p.setFirstName(req.getFirstName());
        p.setFamilyName(req.getFamilyName());
        p.setSpecialtyCode(req.getSpecialtyCode());
        p.setRole(req.getRole() != null ? req.getRole() : Practitioner.Role.DOCTOR);
        return p;
    }
}
