package com.amins.nphies.organization;

import com.amins.nphies.organization.dto.OrganizationRequest;
import com.amins.nphies.organization.dto.OrganizationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository repository;

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listAll(String tenantId) {
        return repository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(OrganizationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(String tenantId, Long id) {
        return repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .map(OrganizationResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    @Transactional
    public OrganizationResponse create(String tenantId, OrganizationRequest req) {
        repository.findByTenantIdAndLicenseNo(tenantId, req.getLicenseNo())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Organization with license " + req.getLicenseNo() + " already exists for this tenant");
                });

        Organization o = mapToEntity(new Organization(), tenantId, req);
        return new OrganizationResponse(repository.save(o));
    }

    @Transactional
    public OrganizationResponse update(String tenantId, Long id, OrganizationRequest req) {
        Organization o = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        if (!o.getLicenseNo().equals(req.getLicenseNo()) &&
                repository.existsByTenantIdAndLicenseNoAndIdNot(tenantId, req.getLicenseNo(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another organization with license " + req.getLicenseNo() + " already exists");
        }

        return new OrganizationResponse(repository.save(mapToEntity(o, tenantId, req)));
    }

    @Transactional
    public void delete(String tenantId, Long id) {
        Organization o = repository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        o.setActive(false);
        repository.save(o);
    }

    private Organization mapToEntity(Organization o, String tenantId, OrganizationRequest req) {
        o.setTenantId(tenantId);
        o.setLicenseNo(req.getLicenseNo());
        o.setOrgType(req.getOrgType() != null ? req.getOrgType() : Organization.OrgType.INSURER);
        o.setName(req.getName());
        return o;
    }
}
