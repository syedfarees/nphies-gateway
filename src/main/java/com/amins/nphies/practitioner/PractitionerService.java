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
    public List<PractitionerResponse> listAll() {
        return repository.findAllByActiveTrue()
                .stream()
                .map(PractitionerResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PractitionerResponse getById(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .map(PractitionerResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner not found"));
    }

    @Transactional
    public PractitionerResponse create(PractitionerRequest req) {
        repository.findByPractitionerLicense(req.getPractitionerLicense())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Practitioner with license " + req.getPractitionerLicense() + " already exists");
                });
        return new PractitionerResponse(repository.save(mapToEntity(new Practitioner(), req)));
    }

    @Transactional
    public PractitionerResponse update(Long id, PractitionerRequest req) {
        Practitioner p = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner not found"));

        if (!p.getPractitionerLicense().equals(req.getPractitionerLicense()) &&
                repository.existsByPractitionerLicenseAndIdNot(req.getPractitionerLicense(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another practitioner with license " + req.getPractitionerLicense() + " already exists");
        }

        return new PractitionerResponse(repository.save(mapToEntity(p, req)));
    }

    @Transactional
    public void delete(Long id) {
        Practitioner p = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner not found"));
        p.setActive(false);
        repository.save(p);
    }

    private Practitioner mapToEntity(Practitioner p, PractitionerRequest req) {
        p.setPractitionerLicense(req.getPractitionerLicense());
        p.setFirstName(req.getFirstName());
        p.setFamilyName(req.getFamilyName());
        p.setSpecialtyCode(req.getSpecialtyCode());
        p.setRole(req.getRole() != null ? req.getRole() : Practitioner.Role.DOCTOR);
        return p;
    }
}
