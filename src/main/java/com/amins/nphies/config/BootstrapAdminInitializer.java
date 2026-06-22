package com.amins.nphies.config;

import com.amins.nphies.config.multitenant.TenantSchemaProvisioner;
import com.amins.nphies.gateway.NphiesEndpoints;
import com.amins.nphies.model.TenantContext;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import com.amins.nphies.security.AesEncryptionService;
import com.amins.nphies.user.User;
import com.amins.nphies.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a bootstrap ADMIN and a placeholder NPHIES config on first boot.
 * Fully idempotent — each step is guarded independently so partial failures
 * on previous boots don't leave the system in an inconsistent state.
 *
 * Override defaults via BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD /
 * BOOTSTRAP_TENANT_ID env vars. Change the password after first login.
 * Replace placeholder NPHIES credentials before real submissions.
 */
@Component
@Slf4j
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final TenantSchemaProvisioner provisioner;
    private final TenantNphiesConfigRepository configRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesEncryptionService;
    private final String adminEmail;
    private final String adminPassword;
    private final String tenantId;

    public BootstrapAdminInitializer(TenantSchemaProvisioner provisioner,
                                     TenantNphiesConfigRepository configRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     AesEncryptionService aesEncryptionService,
                                     @Value("${app.bootstrap.admin-email}") String adminEmail,
                                     @Value("${app.bootstrap.admin-password}") String adminPassword,
                                     @Value("${app.bootstrap.tenant-id}") String tenantId) {
        this.provisioner          = provisioner;
        this.configRepository     = configRepository;
        this.userRepository       = userRepository;
        this.passwordEncoder      = passwordEncoder;
        this.aesEncryptionService = aesEncryptionService;
        this.adminEmail           = adminEmail;
        this.adminPassword        = adminPassword;
        this.tenantId             = tenantId;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Idempotent: provision tenant schema only if not already registered.
        if (!provisioner.exists(tenantId)) {
            provisioner.provision(tenantId, "Bootstrap Tenant");
        }

        // Route all subsequent JPA calls to the bootstrap tenant schema.
        TenantContext.set(tenantId);
        try {
            if (!configRepository.existsById(tenantId)) {
                TenantNphiesConfig config = TenantNphiesConfig.builder()
                        .tenantId(tenantId)
                        .clientId("CHANGE-ME")
                        .clientSecretEncrypted(aesEncryptionService.encrypt("CHANGE-ME"))
                        .providerLicenseNo("PR-FHIR")
                        .tokenEndpoint(NphiesEndpoints.UAT_TOKEN_URL)
                        .apiBaseUrl("https://HSB.nphies.sa/r4")
                        .bundleSubmitUrl("http://176.105.150.83/$process-message")
                        .skipTokenAuth(true)
                        .environment(TenantNphiesConfig.NphiesEnvironment.UAT)
                        .build();
                configRepository.save(config);
                log.warn("Bootstrap: created placeholder NPHIES config for tenant '{}' — replace credentials before submitting to NPHIES", tenantId);
            }

            if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
                User admin = new User();
                admin.setName("Bootstrap Admin");
                admin.setEmail(adminEmail.toLowerCase());
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                log.warn("Bootstrap: created ADMIN user '{}' for tenant '{}'. Log in and change this password immediately.", adminEmail, tenantId);
            }
        } finally {
            TenantContext.clear();
        }
    }
}
