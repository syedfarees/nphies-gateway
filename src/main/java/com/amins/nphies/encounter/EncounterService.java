package com.amins.nphies.encounter;

import com.amins.nphies.encounter.dto.EncounterRequest;
import com.amins.nphies.encounter.dto.EncounterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EncounterService {

    private final EncounterRepository repository;

    @Transactional(readOnly = true)
    public List<EncounterResponse> listAll() {
        return repository.findAllByActiveTrue()
                .stream()
                .map(EncounterResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EncounterResponse getById(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .map(EncounterResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> listByBeneficiary(Long beneficiaryId) {
        return repository.findAllByBeneficiaryIdAndActiveTrue(beneficiaryId)
                .stream()
                .map(EncounterResponse::new)
                .toList();
    }

    @Transactional
    public EncounterResponse create(EncounterRequest req) {
        return new EncounterResponse(repository.save(mapToEntity(new Encounter(), req)));
    }

    @Transactional
    public EncounterResponse update(Long id, EncounterRequest req) {
        Encounter e = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        return new EncounterResponse(repository.save(mapToEntity(e, req)));
    }

    @Transactional
    public void delete(Long id) {
        Encounter e = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        e.setActive(false);
        repository.save(e);
    }

    private Encounter mapToEntity(Encounter e, EncounterRequest req) {
        e.setBeneficiaryId(req.getBeneficiaryId());
        e.setPractitionerId(req.getPractitionerId());
        e.setEncounterClass(req.getEncounterClass() != null ? req.getEncounterClass() : Encounter.EncounterClass.AMB);
        e.setServiceType(req.getServiceType());
        e.setPriority(req.getPriority() != null ? req.getPriority() : "normal");
        e.setPeriodStart(req.getPeriodStart());
        e.setPeriodEnd(req.getPeriodEnd());
        e.setAdmissionSource(req.getAdmissionSource());
        e.setDischargeDisposition(req.getDischargeDisposition());
        e.setServiceProviderId(req.getServiceProviderId());
        e.setStatus(req.getStatus() != null ? req.getStatus() : "finished");
        return e;
    }
}
