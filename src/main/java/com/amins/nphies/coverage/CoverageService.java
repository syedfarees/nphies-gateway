package com.amins.nphies.coverage;

import com.amins.nphies.coverage.dto.CoverageRequest;
import com.amins.nphies.coverage.dto.CoverageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoverageService {

    private final CoverageRepository repository;

    @Transactional(readOnly = true)
    public List<CoverageResponse> listAll(String tenantId) {
        return repository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(CoverageResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public CoverageResponse getById(String tenantId, Long id) {
        return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .map(CoverageResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coverage not found"));
    }

    @Transactional(readOnly = true)
    public List<CoverageResponse> listByBeneficiary(String tenantId, Long beneficiaryId) {
        return repository.findAllByBeneficiaryIdAndTenantIdAndActiveTrue(beneficiaryId, tenantId)
                .stream()
                .map(CoverageResponse::new)
                .toList();
    }

    @Transactional
    public CoverageResponse create(String tenantId, CoverageRequest req) {
        Coverage c = mapToEntity(new Coverage(), tenantId, req);
        return new CoverageResponse(repository.save(c));
    }

    @Transactional
    public CoverageResponse update(String tenantId, Long id, CoverageRequest req) {
        Coverage c = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coverage not found"));
        return new CoverageResponse(repository.save(mapToEntity(c, tenantId, req)));
    }

    @Transactional
    public void delete(String tenantId, Long id) {
        Coverage c = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coverage not found"));
        c.setActive(false);
        repository.save(c);
    }

    private Coverage mapToEntity(Coverage c, String tenantId, CoverageRequest req) {
        c.setTenantId(tenantId);
        c.setBeneficiaryId(req.getBeneficiaryId());
        c.setMemberId(req.getMemberId());
        c.setSubscriberId(req.getSubscriberId());
        c.setPayerLicenseNo(req.getPayerLicenseNo());
        c.setPayerName(req.getPayerName());
        c.setCoverageRelationship(req.getCoverageRelationship() != null ? req.getCoverageRelationship() : "self");
        c.setPeriodStart(req.getPeriodStart());
        c.setPeriodEnd(req.getPeriodEnd());
        c.setClassValue(req.getClassValue());
        c.setClassName(req.getClassName());
        c.setOrderOfBenefit(req.getOrderOfBenefit());
        c.setStatus(req.getStatus() != null ? req.getStatus() : Coverage.Status.ACTIVE);
        return c;
    }
}
