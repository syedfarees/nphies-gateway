package com.amins.nphies.tenant;

import com.amins.nphies.beneficiary.TracareAppClient;
import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import com.amins.nphies.security.AesEncryptionService;
import com.amins.nphies.user.User;
import com.amins.nphies.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantRegistrationService {

    private final TenantNphiesConfigRepository configRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesEncryptionService;
    private final TracareAppClient tracareAppClient;

    @Transactional
    public TenantRegistrationResponse register(TenantRegistrationRequest req) {
        String tenantId = derivetenantId(req.clinicName());

        if (configRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant '" + tenantId + "' already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(req.adminEmail())) {
            throw new IllegalArgumentException("Email '" + req.adminEmail() + "' is already registered");
        }

        // Verify the matching tenant exists in TracareApp clinical system
        boolean tracareLinked = tracareAppClient.lookupTenant(tenantId).isPresent();
        if (!tracareLinked) {
            log.warn("Registering tenant '{}' in ClaimManagement but no matching tenant found in TraCare. " +
                    "Patient lookup will fail until the TraCare tenant is created.", tenantId);
        }

        TenantNphiesConfig config = TenantNphiesConfig.builder()
                .tenantId(tenantId)
                .clientId(req.nphiesClientId())
                .clientSecretEncrypted(aesEncryptionService.encrypt(req.nphiesClientSecret()))
                .providerLicenseNo(req.providerLicenseNo())
                .tokenEndpoint(req.tokenEndpoint())
                .apiBaseUrl(req.apiBaseUrl())
                .environment(req.environment())
                .build();
        configRepository.save(config);

        User admin = new User();
        admin.setName(req.adminName());
        admin.setEmail(req.adminEmail().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(req.adminPassword()));
        admin.setRole("ADMIN");
        admin.setTenantId(tenantId);
        userRepository.save(admin);

        String message = tracareLinked
                ? "Tenant registered and linked to TraCare clinical system."
                : "Tenant registered. Warning: no matching TraCare tenant found — create '" + tenantId + "' in TraCare to enable patient lookup.";

        log.info("Tenant '{}' registered. TraCare linked={}", tenantId, tracareLinked);
        return new TenantRegistrationResponse(tenantId, tracareLinked, message);
    }

    // Derives the tenant ID using the same convention as TraCare: tenant_<clinicname>
    static String derivetenantId(String clinicName) {
        return "tenant_" + clinicName.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
