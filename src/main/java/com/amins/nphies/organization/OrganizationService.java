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
    public List<OrganizationResponse> listAll() {
        return repository.findAllByActiveTrue()
                .stream()
                .map(OrganizationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .map(OrganizationResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest req) {
        repository.findByLicenseNo(req.getLicenseNo())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Organization with license " + req.getLicenseNo() + " already exists");
                });
        return new OrganizationResponse(repository.save(mapToEntity(new Organization(), req)));
    }

    @Transactional
    public OrganizationResponse update(Long id, OrganizationRequest req) {
        Organization o = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        if (!o.getLicenseNo().equals(req.getLicenseNo()) &&
                repository.existsByLicenseNoAndIdNot(req.getLicenseNo(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another organization with license " + req.getLicenseNo() + " already exists");
        }

        return new OrganizationResponse(repository.save(mapToEntity(o, req)));
    }

    @Transactional
    public void delete(Long id) {
        Organization o = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        o.setActive(false);
        repository.save(o);
    }

    private Organization mapToEntity(Organization o, OrganizationRequest req) {
        o.setLicenseNo(req.getLicenseNo());
        o.setOrgType(req.getOrgType() != null ? req.getOrgType() : Organization.OrgType.INSURER);
        o.setName(req.getName());
        return o;
    }
}
