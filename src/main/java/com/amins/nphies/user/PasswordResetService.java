package com.amins.nphies.user;

import com.amins.nphies.mail.EmailService;
import com.amins.nphies.user.dto.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Forgot-password flow.
 *
 * requestReset() never reveals whether an account exists: it is a silent no-op
 * for unknown emails. For known users it stores a hashed 6-digit OTP (15 min
 * validity, 5 attempts) and emails it. Unlike invites, the OTP is NEVER
 * returned to the caller — that would let anyone reset anyone's password.
 * With mail disabled (dev) the OTP is logged instead.
 *
 * The token row is committed via TransactionTemplate BEFORE the email is sent,
 * so a slow SMTP server never holds a DB transaction open.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration VALIDITY = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    public void requestReset(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            log.info("Password reset requested for unknown email — ignoring");
            return;
        }

        String otp = "%06d".formatted(RANDOM.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiresAt(OffsetDateTime.now().plus(VALIDITY));

        // Commit first, email after — SMTP latency must not hold the transaction
        transactionTemplate.executeWithoutResult(tx -> {
            tokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId());
            tokenRepository.save(token);
        });

        boolean sent = emailService.sendPasswordReset(user.getEmail(), otp, token.getExpiresAt());
        if (!sent && !emailService.isEnabled()) {
            // Dev convenience only — with mail enabled the OTP is never logged
            log.warn("Mail disabled — password reset OTP for {} (dev only): {}", user.getEmail(), otp);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        // Same generic message for every failure mode — no account enumeration
        IllegalArgumentException rejected =
                new IllegalArgumentException("Invalid or expired reset code");

        User user = userRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> rejected);
        PasswordResetToken token = tokenRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> rejected);

        if (token.getExpiresAt().isBefore(OffsetDateTime.now()) || token.getAttempts() >= MAX_ATTEMPTS) {
            throw rejected;
        }
        if (!passwordEncoder.matches(req.getOtp(), token.getOtpHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw rejected;
        }

        token.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(token);
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        log.info("Password reset completed for user {}", user.getEmail());
    }
}
