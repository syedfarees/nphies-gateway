package com.amins.nphies.config;

import com.amins.nphies.config.multitenant.TenantSchemaProvisioner;
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
 * Seeds a bootstrap ADMIN and a placeholder NPHIES config on first boot
 * (when the system tenant registry is empty). Runs only once — becomes a
 * no-op once any tenant is registered.
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
        if (!provisioner.isEmpty()) {
            return;
        }

        // Provision the bootstrap tenant schema (creates schema + runs Flyway migrations)
        provisioner.provision(tenantId, "Bootstrap Tenant");

        // Route all subsequent JPA calls to the bootstrap tenant schema
        TenantContext.set(tenantId);
        try {
            if (!configRepository.existsById(tenantId)) {
                TenantNphiesConfig config = TenantNphiesConfig.builder()
                        .tenantId(tenantId)
                        .clientId("CHANGE-ME")
                        .clientSecretEncrypted(aesEncryptionService.encrypt("CHANGE-ME"))
                        .providerLicenseNo("PR-0000-00000")
                        .tokenEndpoint("https://sso.nphies.sa/auth/realms/sehati/protocol/openid-connect/token")
                        .apiBaseUrl("https://HSB.nphies.sa/r4")
                        .environment(TenantNphiesConfig.NphiesEnvironment.UAT)
                        .build();
                configRepository.save(config);
                log.warn("Bootstrap: created placeholder NPHIES config for tenant '{}' — replace credentials before submitting to NPHIES", tenantId);
            }

            User admin = new User();
            admin.setName("Bootstrap Admin");
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);

            log.warn("Bootstrap: created ADMIN user '{}' for tenant '{}'. Log in and change this password immediately.", adminEmail, tenantId);
        } finally {
            TenantContext.clear();
        }
    }
}
