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
    public List<EncounterResponse> listAll(String tenantId) {
        return repository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(EncounterResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EncounterResponse getById(String tenantId, Long id) {
        return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .map(EncounterResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> listByBeneficiary(String tenantId, Long beneficiaryId) {
        return repository.findAllByBeneficiaryIdAndTenantIdAndActiveTrue(beneficiaryId, tenantId)
                .stream()
                .map(EncounterResponse::new)
                .toList();
    }

    @Transactional
    public EncounterResponse create(String tenantId, EncounterRequest req) {
        Encounter e = mapToEntity(new Encounter(), tenantId, req);
        return new EncounterResponse(repository.save(e));
    }

    @Transactional
    public EncounterResponse update(String tenantId, Long id, EncounterRequest req) {
        Encounter e = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        return new EncounterResponse(repository.save(mapToEntity(e, tenantId, req)));
    }

    @Transactional
    public void delete(String tenantId, Long id) {
        Encounter e = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        e.setActive(false);
        repository.save(e);
    }

    private Encounter mapToEntity(Encounter e, String tenantId, EncounterRequest req) {
        e.setTenantId(tenantId);
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
