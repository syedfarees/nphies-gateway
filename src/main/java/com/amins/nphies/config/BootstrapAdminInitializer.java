package com.amins.nphies.config;

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
import org.springframework.transaction.annotation.Transactional;

/**
 * Solves the invite-only chicken-and-egg: registration requires an invitation
 * from a tenant ADMIN, so a brand-new installation (empty users table) gets a
 * bootstrap ADMIN seeded at startup, along with a placeholder tenant config row
 * (invitations FK-reference the tenant). Runs only when no users exist — once
 * real users are present it is a no-op forever.
 *
 * Override the defaults in production via BOOTSTRAP_ADMIN_EMAIL /
 * BOOTSTRAP_ADMIN_PASSWORD / BOOTSTRAP_TENANT_ID, and change the password
 * after first login. The seeded NPHIES credentials are placeholders and must
 * be replaced with real CCHI-issued values before any NPHIES call.
 */
@Component
@Slf4j
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final TenantNphiesConfigRepository configRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesEncryptionService;
    private final String adminEmail;
    private final String adminPassword;
    private final String tenantId;

    public BootstrapAdminInitializer(UserRepository userRepository,
                                     TenantNphiesConfigRepository configRepository,
                                     PasswordEncoder passwordEncoder,
                                     AesEncryptionService aesEncryptionService,
                                     @Value("${app.bootstrap.admin-email}") String adminEmail,
                                     @Value("${app.bootstrap.admin-password}") String adminPassword,
                                     @Value("${app.bootstrap.tenant-id}") String tenantId) {
        this.userRepository       = userRepository;
        this.configRepository     = configRepository;
        this.passwordEncoder      = passwordEncoder;
        this.aesEncryptionService = aesEncryptionService;
        this.adminEmail           = adminEmail;
        this.adminPassword        = adminPassword;
        this.tenantId             = tenantId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

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
            log.warn("Bootstrap: created placeholder NPHIES config for tenant '{}' — replace client credentials before submitting to NPHIES", tenantId);
        }

        User admin = new User();
        admin.setName("Bootstrap Admin");
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setTenantId(tenantId);
        userRepository.save(admin);

        log.warn("Bootstrap: created ADMIN user '{}' for tenant '{}'. " +
                "Log in and change this password immediately.", adminEmail, tenantId);
    }
}
