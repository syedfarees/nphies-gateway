package com.amins.nphies.beneficiary;

import com.amins.nphies.beneficiary.dto.BeneficiaryRequest;
import com.amins.nphies.beneficiary.dto.BeneficiaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository repository;

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listAll(String tenantId) {
        return repository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(BeneficiaryResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getById(String tenantId, Long id) {
        return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .map(BeneficiaryResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getByNationalId(String tenantId, String nationalId) {
        return repository.findByTenantIdAndNationalId(tenantId, nationalId)
                .filter(Beneficiary::isActive)
                .map(BeneficiaryResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
    }

    @Transactional
    public BeneficiaryResponse create(String tenantId, BeneficiaryRequest req) {
        repository.findByTenantIdAndNationalId(tenantId, req.getNationalId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Beneficiary with national ID " + req.getNationalId() + " already exists for this tenant");
        });

        Beneficiary b = mapToEntity(new Beneficiary(), tenantId, req);
        return new BeneficiaryResponse(repository.save(b));
    }

    @Transactional
    public BeneficiaryResponse update(String tenantId, Long id, BeneficiaryRequest req) {
        Beneficiary b = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));

        if (!b.getNationalId().equals(req.getNationalId()) &&
                repository.existsByTenantIdAndNationalIdAndIdNot(tenantId, req.getNationalId(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another beneficiary with national ID " + req.getNationalId() + " already exists");
        }

        return new BeneficiaryResponse(repository.save(mapToEntity(b, tenantId, req)));
    }

    /** Create the beneficiary if it doesn't exist yet; return existing record if nationalId already registered. */
    @Transactional
    public BeneficiaryResponse upsert(String tenantId, BeneficiaryRequest req) {
        return repository.findByTenantIdAndNationalId(tenantId, req.getNationalId())
                .map(existing -> new BeneficiaryResponse(repository.save(mapToEntity(existing, tenantId, req))))
                .orElseGet(() -> new BeneficiaryResponse(repository.save(mapToEntity(new Beneficiary(), tenantId, req))));
    }

    @Transactional
    public void delete(String tenantId, Long id) {
        Beneficiary b = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
        b.setActive(false);
        repository.save(b);
    }

    private Beneficiary mapToEntity(Beneficiary b, String tenantId, BeneficiaryRequest req) {
        b.setTenantId(tenantId);
        b.setNationalId(req.getNationalId());
        b.setIdType(req.getIdType() != null ? req.getIdType() : Beneficiary.IdType.NATIONAL_ID);
        b.setFirstName(req.getFirstName());
        b.setFamilyName(req.getFamilyName());
        b.setDateOfBirth(req.getDateOfBirth());
        b.setGender(req.getGender());
        b.setMemberId(req.getMemberId());
        b.setPayerLicenseNo(req.getPayerLicenseNo());
        b.setPayerName(req.getPayerName());
        b.setCoverageRelationship(req.getCoverageRelationship() != null ? req.getCoverageRelationship() : "self");
        b.setPhone(req.getPhone());
        b.setEmail(req.getEmail());
        return b;
    }
}
