package com.amins.nphies.user;

import com.amins.nphies.mail.EmailService;
import com.amins.nphies.user.dto.InviteRequest;
import com.amins.nphies.user.dto.InviteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Invite-based user creation.
 *
 * A tenant ADMIN issues an invitation for an email address; the invitation
 * carries the admin's tenant and the target role, plus a hashed one-time code.
 * Registration only succeeds with a matching, unexpired, unused code — so the
 * tenant a user lands in is decided by the inviting admin, never by the client.
 *
 * The OTP is emailed to the invitee when mail is enabled; otherwise (or on
 * send failure) it is returned once in the invite response for out-of-band
 * delivery by the admin. The invitation row is committed BEFORE the email is
 * sent so slow SMTP servers never hold a DB transaction open.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration VALIDITY = Duration.ofHours(48);
    private static final int MAX_ATTEMPTS = 5;
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    public InviteResponse invite(User inviter, InviteRequest req) {
        if (inviter.getTenantId() == null || inviter.getTenantId().isBlank()) {
            throw new IllegalArgumentException("Inviting user has no tenant assigned");
        }
        String role = req.getRole() == null || req.getRole().isBlank() ? "USER" : req.getRole().toUpperCase();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Role must be USER or ADMIN");
        }
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        String otp = generateOtp();
        UserInvitation invitation = new UserInvitation();
        invitation.setTenantId(inviter.getTenantId());
        invitation.setEmail(req.getEmail().toLowerCase());
        invitation.setRole(role);
        invitation.setOtpHash(passwordEncoder.encode(otp));
        invitation.setInvitedBy(inviter.getId());
        invitation.setExpiresAt(OffsetDateTime.now().plus(VALIDITY));

        // Commit first, email after — SMTP latency must not hold the transaction.
        // A fresh invite supersedes any pending one for the same email.
        transactionTemplate.executeWithoutResult(tx -> {
            invitationRepository.deleteByEmailIgnoreCaseAndAcceptedAtIsNull(req.getEmail());
            invitationRepository.save(invitation);
        });

        // OTP is only echoed back when it could not be emailed (mail disabled or send failure)
        boolean emailSent = emailService.sendInvite(invitation.getEmail(), otp, invitation.getExpiresAt());
        return new InviteResponse(invitation.getEmail(), emailSent ? null : otp,
                invitation.getExpiresAt(), emailSent);
    }

    /**
     * Called from the public registration page: (re)sends the OTP for a pending
     * invitation to the invitee's email. Rotates the code and resets the attempt
     * counter. Silent no-op when no pending invitation exists — never reveals
     * whether an email was invited. The OTP is never returned to the caller
     * (possession of the mailbox is the proof of identity); with mail disabled
     * (dev) it is logged instead.
     */
    public void resendOtp(String email) {
        UserInvitation invitation = invitationRepository
                .findTopByEmailIgnoreCaseAndAcceptedAtIsNullOrderByCreatedAtDesc(email)
                .orElse(null);
        if (invitation == null) {
            log.info("Registration OTP requested for email without pending invitation — ignoring");
            return;
        }

        String otp = generateOtp();
        invitation.setOtpHash(passwordEncoder.encode(otp));
        invitation.setAttempts(0);
        invitation.setExpiresAt(OffsetDateTime.now().plus(VALIDITY));

        // Commit first, email after — SMTP latency must not hold the transaction
        transactionTemplate.executeWithoutResult(tx -> invitationRepository.save(invitation));

        boolean sent = emailService.sendInvite(invitation.getEmail(), otp, invitation.getExpiresAt());
        if (!sent && !emailService.isEnabled()) {
            // Dev convenience only — with mail enabled the OTP is never logged
            log.warn("Mail disabled — registration OTP for {} (dev only): {}", invitation.getEmail(), otp);
        }
    }

    @Transactional
    public UserInvitation redeem(String email, String otp) {
        UserInvitation invitation = invitationRepository
                .findTopByEmailIgnoreCaseAndAcceptedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No invitation found for this email — ask your administrator for one"));

        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Invitation expired — ask your administrator for a new one");
        }
        if (invitation.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Too many incorrect codes — ask your administrator for a new invitation");
        }
        if (!passwordEncoder.matches(otp, invitation.getOtpHash())) {
            invitation.setAttempts(invitation.getAttempts() + 1);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invalid invitation code");
        }

        invitation.setAcceptedAt(OffsetDateTime.now());
        return invitationRepository.save(invitation);
    }

    private static String generateOtp() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }
}
