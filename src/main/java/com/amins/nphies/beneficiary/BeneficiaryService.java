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
    public List<BeneficiaryResponse> listAll() {
        return repository.findAllByActiveTrue()
                .stream()
                .map(BeneficiaryResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getById(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .map(BeneficiaryResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getByNationalId(String nationalId) {
        return repository.findByNationalId(nationalId)
                .filter(Beneficiary::isActive)
                .map(BeneficiaryResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
    }

    @Transactional
    public BeneficiaryResponse create(BeneficiaryRequest req) {
        repository.findByNationalId(req.getNationalId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Beneficiary with national ID " + req.getNationalId() + " already exists");
        });
        return new BeneficiaryResponse(repository.save(mapToEntity(new Beneficiary(), req)));
    }

    @Transactional
    public BeneficiaryResponse update(Long id, BeneficiaryRequest req) {
        Beneficiary b = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));

        if (!b.getNationalId().equals(req.getNationalId()) &&
                repository.existsByNationalIdAndIdNot(req.getNationalId(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another beneficiary with national ID " + req.getNationalId() + " already exists");
        }

        return new BeneficiaryResponse(repository.save(mapToEntity(b, req)));
    }

    @Transactional
    public BeneficiaryResponse upsert(BeneficiaryRequest req) {
        return repository.findByNationalId(req.getNationalId())
                .map(existing -> new BeneficiaryResponse(repository.save(mapToEntity(existing, req))))
                .orElseGet(() -> new BeneficiaryResponse(repository.save(mapToEntity(new Beneficiary(), req))));
    }

    @Transactional
    public void delete(Long id) {
        Beneficiary b = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
        b.setActive(false);
        repository.save(b);
    }

    private Beneficiary mapToEntity(Beneficiary b, BeneficiaryRequest req) {
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
