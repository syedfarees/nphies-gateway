package com.amins.nphies.user;

import com.amins.nphies.mail.EmailService;
import com.amins.nphies.user.dto.ResetPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private TransactionTemplate transactionTemplate;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                tokenRepository, userRepository, passwordEncoder, emailService, transactionTemplate);
        user = new User();
        user.setId(7L);
        user.setEmail("doctor@hospital.sa");
        user.setPasswordHash("old-hash");
    }

    private void runTransactionCallbacks() {
        doAnswer(inv -> {
            inv.<Consumer<TransactionStatus>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void requestReset_unknownEmail_isSilentNoOp() {
        when(userRepository.findByEmailIgnoreCase("ghost@nowhere.sa")).thenReturn(Optional.empty());

        service.requestReset("ghost@nowhere.sa");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void requestReset_knownEmail_storesHashedOtpAndEmails() {
        runTransactionCallbacks();
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("otp-hash");
        when(emailService.sendPasswordReset(eq(user.getEmail()), anyString(), any())).thenReturn(true);

        service.requestReset(user.getEmail());

        verify(tokenRepository).deleteByUserIdAndUsedAtIsNull(7L);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getOtpHash()).isEqualTo("otp-hash");
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
        verify(emailService).sendPasswordReset(eq(user.getEmail()), anyString(), any());
    }

    @Test
    void resetPassword_validOtp_updatesPasswordAndMarksTokenUsed() {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        PasswordResetToken token = pendingToken();
        when(tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "otp-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password-1")).thenReturn("new-hash");

        service.resetPassword(request("123456", "new-password-1"));

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_wrongOtp_incrementsAttemptsAndRejects() {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        PasswordResetToken token = pendingToken();
        when(tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("999999", "otp-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.resetPassword(request("999999", "new-password-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");

        assertThat(token.getAttempts()).isEqualTo(1);
        verify(tokenRepository).save(token);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_expiredToken_rejects() {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        PasswordResetToken token = pendingToken();
        token.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(tokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(request("123456", "new-password-1")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_unknownEmail_rejectsWithSameGenericMessage() {
        when(userRepository.findByEmailIgnoreCase("ghost@nowhere.sa")).thenReturn(Optional.empty());

        ResetPasswordRequest req = request("123456", "new-password-1");
        req.setEmail("ghost@nowhere.sa");
        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");
    }

    private PasswordResetToken pendingToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(7L);
        token.setOtpHash("otp-hash");
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        return token;
    }

    private ResetPasswordRequest request(String otp, String newPassword) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(user.getEmail());
        req.setOtp(otp);
        req.setNewPassword(newPassword);
        return req;
    }
}
