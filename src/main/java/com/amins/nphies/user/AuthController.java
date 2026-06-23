package com.amins.nphies.user;

import com.amins.nphies.config.multitenant.TenantSchemaProvisioner;
import com.amins.nphies.model.TenantContext;
import com.amins.nphies.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final InvitationService invitationService;
    private final PasswordResetService passwordResetService;
    private final TenantSchemaProvisioner tenantSchemaProvisioner;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        if (!tenantSchemaProvisioner.exists(req.getTenantId())) {
            return ResponseEntity.status(401).body("{\"message\":\"Invalid credentials\"}");
        }
        TenantContext.set(req.getTenantId());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("{\"message\":\"Invalid credentials\"}");
        }
        User user = userService.findByEmail(req.getEmail());
        String token = jwtService.generateToken(user.getEmail(), req.getTenantId());
        return ResponseEntity.ok(new AuthResponse(user.getName(), user.getEmail(), token, user.getRole(), req.getTenantId()));
    }

    /**
     * (Re)sends the registration OTP to the invitee's email. Always returns
     * success — never reveals whether the email has a pending invitation.
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<RegisterResponse> sendRegistrationOtp(@Valid @RequestBody SendOtpRequest req) {
        if (!tenantSchemaProvisioner.exists(req.getTenantId())) {
            return ResponseEntity.ok(new RegisterResponse(true)); // silent no-op for unknown tenants
        }
        TenantContext.set(req.getTenantId());
        invitationService.resendOtp(req.getEmail());
        return ResponseEntity.ok(new RegisterResponse(true));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (!tenantSchemaProvisioner.exists(req.getTenantId())) {
            return ResponseEntity.badRequest().body("{\"message\":\"Invalid or unknown tenant\"}");
        }
        TenantContext.set(req.getTenantId());
        try {
            userService.register(req);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"message\":\"" + e.getMessage() + "\"}");
        }
        return ResponseEntity.ok(new RegisterResponse(true));
    }

    /** ADMIN-only (enforced in SecurityConfig). Invites a user into the admin's own tenant. */
    @PostMapping("/invite")
    public ResponseEntity<?> invite(@AuthenticationPrincipal User inviter,
                                    @Valid @RequestBody InviteRequest req) {
        try {
            return ResponseEntity.ok(invitationService.invite(inviter, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /** Always returns success — never reveals whether the email has an account. */
    @PostMapping("/forgot-password")
    public ResponseEntity<RegisterResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        if (!tenantSchemaProvisioner.exists(req.getTenantId())) {
            return ResponseEntity.ok(new RegisterResponse(true)); // silent no-op for unknown tenants
        }
        TenantContext.set(req.getTenantId());
        passwordResetService.requestReset(req.getEmail());
        return ResponseEntity.ok(new RegisterResponse(true));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        if (!tenantSchemaProvisioner.exists(req.getTenantId())) {
            return ResponseEntity.badRequest().body("{\"message\":\"Invalid or unknown tenant\"}");
        }
        TenantContext.set(req.getTenantId());
        try {
            passwordResetService.resetPassword(req);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"message\":\"" + e.getMessage() + "\"}");
        }
        return ResponseEntity.ok(new RegisterResponse(true));
    }
}
